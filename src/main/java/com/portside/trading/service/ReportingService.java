package com.portside.trading.service;

import com.portside.trading.domain.*;
import com.portside.trading.repo.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Aggregate views for the dashboard and the reporting screens (dashboard/trial-balance/
 *  general-ledger/container-profitability/receivables-aging/stock), mirroring the shapes the
 *  prototype's listVals() built for 'gl'/'tb'/'profit'/'receivables'/'stock'/'dash'. */
@Service
public class ReportingService {

    private final StockService stockService;
    private final SalesInvoiceRepository invoiceRepository;
    private final ReceiptService receiptService;
    private final CustomerRepository customerRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final PurchaseOrderRepository poRepository;
    private final InvoiceCalcService invoiceCalcService;

    public ReportingService(StockService stockService, SalesInvoiceRepository invoiceRepository,
                             ReceiptService receiptService, CustomerRepository customerRepository,
                             ChartOfAccountRepository chartOfAccountRepository,
                             JournalEntryRepository journalEntryRepository, PurchaseOrderRepository poRepository,
                             InvoiceCalcService invoiceCalcService) {
        this.stockService = stockService;
        this.invoiceRepository = invoiceRepository;
        this.receiptService = receiptService;
        this.customerRepository = customerRepository;
        this.chartOfAccountRepository = chartOfAccountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.poRepository = poRepository;
        this.invoiceCalcService = invoiceCalcService;
    }

    // ---------- trial balance (a REAL trial balance off journal_line, not a synthetic estimate) ----------
    public record AccountBalance(String code, String name, String type, double debit, double credit, double net) {}
    public record TrialBalance(List<AccountBalance> accounts, double totalDebit, double totalCredit) {}

    public TrialBalance trialBalance() {
        Map<String, double[]> sums = new HashMap<>();
        for (JournalEntry je : journalEntryRepository.findAll()) {
            for (JournalLine jl : je.getLines()) {
                double[] s = sums.computeIfAbsent(jl.getAccount().getCode(), k -> new double[2]);
                s[0] += jl.getDebit();
                s[1] += jl.getCredit();
            }
        }
        List<AccountBalance> accounts = new ArrayList<>();
        for (ChartOfAccount a : chartOfAccountRepository.findAll()) {
            double[] s = sums.getOrDefault(a.getCode(), new double[2]);
            accounts.add(new AccountBalance(a.getCode(), a.getName(), a.getType(), s[0], s[1], s[0] - s[1]));
        }
        accounts.sort(Comparator.comparing(AccountBalance::code));
        double td = accounts.stream().mapToDouble(AccountBalance::debit).sum();
        double tc = accounts.stream().mapToDouble(AccountBalance::credit).sum();
        return new TrialBalance(accounts, td, tc);
    }

    // ---------- general ledger (flattened voucher lines, newest first) ----------
    public record GlLine(String voucherNo, String date, String narration, String accountCode, String accountName,
                          double debit, double credit) {}

    public List<GlLine> generalLedger(int limit) {
        List<GlLine> out = new ArrayList<>();
        for (JournalEntry je : journalEntryRepository.findAllNewestFirst()) {
            for (JournalLine jl : je.getLines()) {
                out.add(new GlLine(je.getVoucherNo(), je.getDate().toString(), je.getNarration(),
                        jl.getAccount().getCode(), jl.getAccount().getName(), jl.getDebit(), jl.getCredit()));
            }
            if (out.size() >= limit) break;
        }
        return out;
    }

    // ---------- container profitability ----------
    public record ContainerProfitability(String containerCode, String status, double landedCost, double revenue,
                                          double cogs, double realisedGp, double marginPct, double unsoldValue,
                                          boolean live, double sellThroughPct) {}

    public List<ContainerProfitability> containerProfitability() {
        List<ContainerProfitability> out = new ArrayList<>();
        for (ContainerLotView lot : stockService.allLots()) {
            double revenue = 0, cogs = 0;
            for (SalesInvoice inv : invoiceRepository.findAll()) {
                if (inv.getStatus() != InvoiceStatus.POSTED) continue;
                for (var line : inv.getLines()) {
                    if (!line.getContainer().getId().equals(lot.container().getId())) continue;
                    revenue += line.getQty() * line.getRatePkr();
                    cogs += line.getQty() * line.getUnitCostSnapshot();
                }
            }
            double stockValue = lot.rows().stream().mapToDouble(LotRowView::stockValue).sum();
            double margin = revenue == 0 ? 0 : (revenue - cogs) / revenue;
            double sellThrough = lot.live() && lot.landedTotal() != 0 ? 1 - stockValue / lot.landedTotal() : 0;
            out.add(new ContainerProfitability(lot.container().getCode(), lot.container().getStatus().name(),
                    lot.landedTotal(), revenue, cogs, revenue - cogs, margin, lot.live() ? stockValue : lot.landedTotal(),
                    lot.live(), sellThrough));
        }
        return out;
    }

    // ---------- receivables & aging ----------
    public record CustomerReceivable(String customerCode, String customerName, String terms, double invoiced,
                                      double received, double balance, int oldestDays, double headroom) {}
    public record AgingBuckets(double d0to15, double d16to30, double d31to45, double d46plus, double total) {}

    public List<CustomerReceivable> receivablesAging() {
        LocalDate today = LocalDate.now();
        List<CustomerReceivable> out = new ArrayList<>();
        for (Customer c : customerRepository.findAll()) {
            double invoiced = 0, received = 0;
            int oldest = 0;
            for (SalesInvoice inv : invoiceRepository.findByCustomer_Id(c.getId())) {
                if (inv.getStatus() != InvoiceStatus.POSTED) continue;
                invoiced += invoiceCalcService.totals(inv).gross();
                received += invoiceCalcService.totals(inv).gross() - receiptService.openBalance(inv);
                if (receiptService.openBalance(inv) > 0.5) {
                    oldest = Math.max(oldest, (int) ChronoUnit.DAYS.between(inv.getDate(), today));
                }
            }
            double balance = invoiced - received;
            out.add(new CustomerReceivable(c.getCode(), c.getName(), c.getPaymentTerms(), invoiced, received,
                    balance, oldest, c.getCreditLimit() - balance));
        }
        return out;
    }

    public AgingBuckets agingBuckets() {
        LocalDate today = LocalDate.now();
        double b0 = 0, b1 = 0, b2 = 0, b3 = 0;
        for (SalesInvoice inv : invoiceRepository.findAll()) {
            if (inv.getStatus() != InvoiceStatus.POSTED) continue;
            double bal = receiptService.openBalance(inv);
            if (bal <= 0.5) continue;
            long days = ChronoUnit.DAYS.between(inv.getDate(), today);
            if (days <= 15) b0 += bal; else if (days <= 30) b1 += bal; else if (days <= 45) b2 += bal; else b3 += bal;
        }
        return new AgingBuckets(b0, b1, b2, b3, b0 + b1 + b2 + b3);
    }

    // ---------- item stock rollup ----------
    public record ItemStock(String itemCode, String itemName, String category, String uom, double received,
                             double sold, double adjustments, double onHand, double avgUnitCost, double stockValue,
                             int lotsHolding) {}

    public List<ItemStock> stockOnHand() {
        record Acc(double[] v) {}
        Map<String, double[]> byItem = new LinkedHashMap<>(); // [received, sold, adj, onHand, value, lots]
        Map<String, String> names = new HashMap<>();
        Map<String, String> cats = new HashMap<>();
        Map<String, String> uoms = new HashMap<>();
        for (ContainerLotView lot : stockService.liveLots()) {
            for (LotRowView r : lot.rows()) {
                double[] acc = byItem.computeIfAbsent(r.itemCode(), k -> new double[6]);
                acc[0] += r.qty();
                acc[1] += r.sold();
                acc[2] += r.adjusted();
                acc[3] += r.onHand();
                acc[4] += r.stockValue();
                if (r.onHand() > 0) acc[5] += 1;
                names.put(r.itemCode(), r.itemName());
                cats.putIfAbsent(r.itemCode(), "");
                uoms.put(r.itemCode(), r.uom());
            }
        }
        List<ItemStock> out = new ArrayList<>();
        for (var e : byItem.entrySet()) {
            double[] v = e.getValue();
            double avgCost = v[3] == 0 ? 0 : v[4] / v[3];
            out.add(new ItemStock(e.getKey(), names.get(e.getKey()), cats.get(e.getKey()), uoms.get(e.getKey()),
                    v[0], v[1], v[2], v[3], avgCost, v[4], (int) v[5]));
        }
        return out;
    }

    // ---------- dashboard ----------
    public record MonthlyFigure(String month, double netSales, double cogs, double grossProfit) {}
    public record DashboardSummary(double totalInventoryValue, double totalInTransitValue, int containerCount,
                                    long containersInTransit, long containersInClearance, long containersSelling,
                                    double monthlySales, double grossProfitYtd, double grossMarginPct,
                                    double totalReceivables, List<MonthlyFigure> salesByMonth) {}

    public DashboardSummary dashboard() {
        var lots = stockService.allLots();
        long inTransit = lots.stream().filter(l -> l.container().getStatus() == ContainerStatus.IN_TRANSIT).count();
        long inClearance = lots.stream().filter(l -> l.container().getStatus() == ContainerStatus.IN_CLEARANCE).count();
        long selling = lots.stream().filter(l -> l.container().getStatus() == ContainerStatus.SELLING).count();

        Map<String, double[]> byMonth = new TreeMap<>();
        double totalNet = 0, totalCogs = 0;
        for (SalesInvoice inv : invoiceRepository.findAll()) {
            if (inv.getStatus() != InvoiceStatus.POSTED) continue;
            var t = invoiceCalcService.totals(inv);
            totalNet += t.net();
            totalCogs += t.cogs();
            String key = inv.getDate().getYear() + "-" + String.format("%02d", inv.getDate().getMonthValue());
            double[] acc = byMonth.computeIfAbsent(key, k -> new double[2]);
            acc[0] += t.net();
            acc[1] += t.cogs();
        }
        List<MonthlyFigure> months = new ArrayList<>();
        for (var e : byMonth.entrySet()) {
            LocalDate d = LocalDate.parse(e.getKey() + "-01");
            String label = d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + d.getYear();
            months.add(new MonthlyFigure(label, e.getValue()[0], e.getValue()[1], e.getValue()[0] - e.getValue()[1]));
        }

        double receivables = customerRepository.findAll().stream()
                .flatMap(c -> invoiceRepository.findByCustomer_Id(c.getId()).stream())
                .filter(i -> i.getStatus() == InvoiceStatus.POSTED)
                .mapToDouble(receiptService::openBalance).sum();

        return new DashboardSummary(stockService.totalStockValue(), stockService.totalInTransitValue(),
                (int) poRepository.count(), inTransit, inClearance, selling,
                months.isEmpty() ? 0 : months.get(months.size() - 1).netSales(),
                totalNet - totalCogs, totalNet == 0 ? 0 : (totalNet - totalCogs) / totalNet, receivables, months);
    }
}
