package com.portside.trading.service;

import com.portside.trading.domain.*;
import com.portside.trading.repo.ContainerRepository;
import com.portside.trading.repo.CustomerRepository;
import com.portside.trading.repo.ItemRepository;
import com.portside.trading.repo.SalesInvoiceRepository;
import com.portside.trading.security.CurrentUserService;
import com.portside.trading.web.dto.SalesInvoiceRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ports the prototype's startInvoice()/postDraft() (lines ~1036-1070): floor-price approval
 *  routing, COGS/GP calculation, and the GL batch a posted invoice writes. */
@Service
public class SalesInvoiceService {

    public static class BelowFloorNoStockException extends RuntimeException {
        public BelowFloorNoStockException(String message) { super(message); }
    }

    private final SalesInvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ItemRepository itemRepository;
    private final ContainerRepository containerRepository;
    private final CodeGeneratorService codeGenerator;
    private final LandedCostService landedCostService;
    private final PricingService pricingService;
    private final SettingsService settingsService;
    private final JournalService journalService;
    private final CurrentUserService currentUser;

    public SalesInvoiceService(SalesInvoiceRepository invoiceRepository, CustomerRepository customerRepository,
                                ItemRepository itemRepository, ContainerRepository containerRepository,
                                CodeGeneratorService codeGenerator, LandedCostService landedCostService,
                                PricingService pricingService, SettingsService settingsService,
                                JournalService journalService, CurrentUserService currentUser) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.itemRepository = itemRepository;
        this.containerRepository = containerRepository;
        this.codeGenerator = codeGenerator;
        this.landedCostService = landedCostService;
        this.pricingService = pricingService;
        this.settingsService = settingsService;
        this.journalService = journalService;
        this.currentUser = currentUser;
    }

    @Transactional
    public SalesInvoice create(SalesInvoiceRequest req) {
        SalesInvoice inv = SalesInvoice.builder()
                .code(codeGenerator.nextInvoiceCode())
                .date(LocalDate.now())
                .status(InvoiceStatus.DRAFT)
                .createdBy(currentUser.username())
                .lines(new ArrayList<>())
                .build();
        applyRequest(inv, req);
        return invoiceRepository.save(inv);
    }

    @Transactional
    public SalesInvoice update(SalesInvoice inv, SalesInvoiceRequest req) {
        if (inv.getStatus() == InvoiceStatus.POSTED) {
            throw new IllegalStateException("Posted invoices can't be edited.");
        }
        applyRequest(inv, req);
        return invoiceRepository.save(inv);
    }

    private void applyRequest(SalesInvoice inv, SalesInvoiceRequest req) {
        if (req.customerCode() != null) {
            inv.setCustomer(customerRepository.findByCode(req.customerCode())
                    .orElseThrow(() -> new IllegalArgumentException("No such customer " + req.customerCode())));
        }
        if (req.lines() != null) {
            inv.getLines().clear();
            for (var l : req.lines()) {
                Item item = itemRepository.findByCode(l.itemCode())
                        .orElseThrow(() -> new IllegalArgumentException("No such item " + l.itemCode()));
                Container container = containerRepository.findByCode(l.containerCode())
                        .orElseThrow(() -> new IllegalArgumentException("No such container " + l.containerCode()));
                inv.getLines().add(SalesInvoiceLine.builder().invoice(inv).item(item).container(container)
                        .qty(l.qty()).ratePkr(l.ratePkr()).unitCostSnapshot(0).build());
            }
        }
    }

    /** @return true if fully posted, false if routed to pending-approval (below floor, non-Owner role) */
    @Transactional
    public boolean post(SalesInvoice inv) {
        if (inv.getStatus() == InvoiceStatus.POSTED) {
            throw new IllegalStateException(inv.getCode() + " is already posted.");
        }
        if (inv.getLines().isEmpty()) {
            throw new IllegalStateException("Add at least one line before posting.");
        }

        boolean belowFloor = false;
        for (var line : inv.getLines()) {
            double floor = pricingService.floorPrice(line.getContainer(), line.getItem());
            if (line.getRatePkr() < floor) belowFloor = true;
            double onHand = landedCostService.onHand(line.getContainer(), line.getItem().getId());
            if (line.getQty() > onHand) {
                throw new IllegalStateException(line.getItem().getName() + " — only " + onHand
                        + " available in " + line.getContainer().getCode() + ", line requests " + line.getQty() + ".");
            }
        }

        if (belowFloor && currentUser.role() != Role.OWNER) {
            inv.setStatus(InvoiceStatus.PENDING_APPROVAL);
            invoiceRepository.save(inv);
            return false;
        }

        for (var line : inv.getLines()) {
            line.setUnitCostSnapshot(landedCostService.unitCost(line.getContainer(), line.getItem().getId()));
        }
        inv.setStatus(InvoiceStatus.POSTED);
        invoiceRepository.save(inv);

        double net = inv.getLines().stream().mapToDouble(l -> l.getQty() * l.getRatePkr()).sum();
        double cogs = inv.getLines().stream().mapToDouble(l -> l.getQty() * l.getUnitCostSnapshot()).sum();
        double tax = net * (settingsService.taxRatePct() / 100);

        journalService.post("JV-S", inv.getDate(), "Sales invoice " + inv.getCode() + " — " + inv.getCustomer().getName(),
                "SALES_INVOICE", inv.getCode(), java.util.List.of(
                        JournalService.Posting.debit("1200", net + tax),
                        JournalService.Posting.credit("4100", net),
                        JournalService.Posting.credit("2200", tax),
                        JournalService.Posting.debit("5100", cogs),
                        JournalService.Posting.credit("1300", cogs)
                ));
        return true;
    }

    /** Owner override: post a below-floor invoice that was routed to pending-approval. */
    @Transactional
    public boolean approve(SalesInvoice inv) {
        if (inv.getStatus() != InvoiceStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only a pending-approval invoice can be approved.");
        }
        inv.setStatus(InvoiceStatus.DRAFT); // fall through the normal post() path as the Owner
        return post(inv);
    }
}
