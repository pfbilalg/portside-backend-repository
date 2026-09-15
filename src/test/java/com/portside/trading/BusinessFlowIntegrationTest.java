package com.portside.trading;

import static org.assertj.core.api.Assertions.assertThat;

import com.portside.trading.domain.*;
import com.portside.trading.repo.*;
import com.portside.trading.service.*;
import com.portside.trading.web.dto.PurchaseOrderRequest;
import com.portside.trading.web.dto.ReceiptRequest;
import com.portside.trading.web.dto.SalesInvoiceRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the full PO -> receive -> landed cost -> sales invoice -> receipt -> trial-balance
 * flow end to end against the real services, verifying the numbers the prototype's business
 * rules promise (landed-cost allocation, COGS/GP, and a balanced double-entry ledger).
 */
@SpringBootTest
@Transactional
@WithMockUser(username = "owner", roles = "OWNER")
class BusinessFlowIntegrationTest {

    @Autowired ItemRepository itemRepository;
    @Autowired SupplierRepository supplierRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired ChartOfAccountRepository chartOfAccountRepository;
    @Autowired AppSettingRepository appSettingRepository;
    @Autowired ContainerRepository containerRepository;

    @Autowired PurchaseOrderService purchaseOrderService;
    @Autowired LandedCostService landedCostService;
    @Autowired SalesInvoiceService salesInvoiceService;
    @Autowired ReceiptService receiptService;
    @Autowired ReportingService reportingService;
    @Autowired SalesInvoiceRepository salesInvoiceRepository;
    @Autowired InvoiceCalcService invoiceCalcService;

    private Item item;
    private Supplier supplier;
    private Customer customer;

    @BeforeEach
    void seed() {
        appSettingRepository.save(new AppSetting("usdRate", "100"));
        appSettingRepository.save(new AppSetting("taxRatePct", "0"));

        for (var a : List.of(
                new ChartOfAccount("1100", "Bank", "ASSET"),
                new ChartOfAccount("1200", "Trade receivables", "ASSET"),
                new ChartOfAccount("1300", "Inventory", "ASSET"),
                new ChartOfAccount("2200", "Sales tax output", "LIABILITY"),
                new ChartOfAccount("4100", "Sales revenue", "REVENUE"),
                new ChartOfAccount("5100", "Cost of goods sold", "EXPENSE"))) {
            chartOfAccountRepository.save(a);
        }

        item = itemRepository.save(Item.builder().code("IT-1").name("Test item").uom("pcs").category("Test").reorderLevel(10).build());
        supplier = supplierRepository.save(Supplier.builder().code("SUP-T").name("Test Supplier").originPort("Testport").paymentTerms("LC at sight").build());
        customer = customerRepository.save(Customer.builder().code("CUS-T").name("Test Customer").city("Karachi").paymentTerms("30 days").creditLimit(1_000_000).standingDiscountPct(0).salesman("Tester").build());
    }

    @Test
    void fullTradingCycleBalancesTheLedger() {
        // PO: 100 units at $10 FOB, usdRate=100 -> FOB PKR = 100,000
        var po = purchaseOrderService.create(new PurchaseOrderRequest(supplier.getCode(), LocalDate.now().toString(),
                "FOB Testport", "LC at sight", "ORDERED", List.of(new PurchaseOrderRequest.Line(item.getCode(), 100, 10))));

        Container container = purchaseOrderService.receive(po);
        // simulate the warehouse GRN step (not yet its own module): goods physically arrive
        container.setReceivedDate(LocalDate.now());
        container.setStatus(ContainerStatus.SELLING);
        container = containerRepository.save(container);

        // expected landed cost on VALUE basis: freight 9% + insurance 1.6% + duty 20% = 30.6% of FOB
        var lot = landedCostService.compute(container);
        double expectedLanded = 100_000 * 1.306;
        assertThat(lot.landedTotal()).isCloseTo(expectedLanded, org.assertj.core.data.Offset.offset(1.0));
        double unitCost = lot.rows().get(0).unitCost();
        assertThat(unitCost).isCloseTo(expectedLanded / 100, org.assertj.core.data.Offset.offset(0.01));

        // sell 10 units at 2,000 PKR (comfortably above the ~1,306 floor-ish unit cost)
        var invoice = salesInvoiceService.create(new SalesInvoiceRequest(customer.getCode(),
                List.of(new SalesInvoiceRequest.Line(item.getCode(), container.getCode(), 10, 2000))));
        boolean posted = salesInvoiceService.post(invoice);
        assertThat(posted).isTrue();

        invoice = salesInvoiceRepository.findByCode(invoice.getCode()).orElseThrow();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.POSTED);
        double expectedCogs = 10 * unitCost;
        double expectedGp = 20_000 - expectedCogs;
        var totals = invoiceCalcService.totals(invoice);
        assertThat(totals.net()).isEqualTo(20_000);
        assertThat(totals.cogs()).isCloseTo(expectedCogs, org.assertj.core.data.Offset.offset(0.1));
        assertThat(totals.gp()).isCloseTo(expectedGp, org.assertj.core.data.Offset.offset(0.1));

        // stock on hand must have dropped by exactly the sold quantity
        assertThat(landedCostService.onHand(container, item.getId())).isEqualTo(90);

        // receive full payment
        receiptService.record(new ReceiptRequest(customer.getCode(), 20_000, "Bank transfer"));

        // the ledger this flow wrote must balance
        var tb = reportingService.trialBalance();
        assertThat(tb.totalDebit()).isCloseTo(tb.totalCredit(), org.assertj.core.data.Offset.offset(0.01));
        assertThat(tb.totalDebit()).isGreaterThan(0);
    }
}
