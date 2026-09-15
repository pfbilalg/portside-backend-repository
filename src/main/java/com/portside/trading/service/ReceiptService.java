package com.portside.trading.service;

import com.portside.trading.domain.Customer;
import com.portside.trading.domain.InvoiceStatus;
import com.portside.trading.domain.Receipt;
import com.portside.trading.domain.ReceiptAllocation;
import com.portside.trading.domain.SalesInvoice;
import com.portside.trading.repo.CustomerRepository;
import com.portside.trading.repo.ReceiptAllocationRepository;
import com.portside.trading.repo.ReceiptRepository;
import com.portside.trading.repo.SalesInvoiceRepository;
import com.portside.trading.web.dto.ReceiptRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ports the prototype's receive() (line ~1071) but allocates oldest-invoice-first instead of
 *  just netting a running customer balance, so receivables/aging stay exact per invoice. */
@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptAllocationRepository allocationRepository;
    private final SalesInvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final CodeGeneratorService codeGenerator;
    private final InvoiceCalcService invoiceCalcService;
    private final JournalService journalService;

    public ReceiptService(ReceiptRepository receiptRepository, ReceiptAllocationRepository allocationRepository,
                           SalesInvoiceRepository invoiceRepository, CustomerRepository customerRepository,
                           CodeGeneratorService codeGenerator, InvoiceCalcService invoiceCalcService,
                           JournalService journalService) {
        this.receiptRepository = receiptRepository;
        this.allocationRepository = allocationRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.codeGenerator = codeGenerator;
        this.invoiceCalcService = invoiceCalcService;
        this.journalService = journalService;
    }

    public double openBalance(SalesInvoice inv) {
        if (inv.getStatus() != InvoiceStatus.POSTED) return 0;
        double gross = invoiceCalcService.totals(inv).gross();
        double applied = allocationRepository.findByInvoice_Id(inv.getId()).stream()
                .mapToDouble(ReceiptAllocation::getAmountApplied).sum();
        return gross - applied;
    }

    @Transactional
    public Receipt record(ReceiptRequest req) {
        if (req.amountPkr() <= 0) throw new IllegalStateException("Receipt amount must be positive.");
        Customer customer = customerRepository.findByCode(req.customerCode())
                .orElseThrow(() -> new IllegalArgumentException("No such customer " + req.customerCode()));

        Receipt receipt = Receipt.builder()
                .code(codeGenerator.nextReceiptCode())
                .date(LocalDate.now())
                .customer(customer)
                .amountPkr(req.amountPkr())
                .mode(req.mode())
                .allocations(new ArrayList<>())
                .build();
        receipt = receiptRepository.save(receipt);

        List<SalesInvoice> open = invoiceRepository.findByCustomer_Id(customer.getId()).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.POSTED)
                .sorted(Comparator.comparing(SalesInvoice::getDate).thenComparing(SalesInvoice::getId))
                .toList();

        double remaining = req.amountPkr();
        for (SalesInvoice inv : open) {
            if (remaining <= 0.005) break;
            double bal = openBalance(inv);
            if (bal <= 0.005) continue;
            double applied = Math.min(bal, remaining);
            remaining -= applied;
            receipt.getAllocations().add(ReceiptAllocation.builder()
                    .receipt(receipt).invoice(inv).amountApplied(applied).build());
        }
        receipt = receiptRepository.save(receipt);

        journalService.post("JV-R", receipt.getDate(), "Receipt " + receipt.getCode() + " — " + customer.getName(),
                "RECEIPT", receipt.getCode(), List.of(
                        JournalService.Posting.debit("1100", receipt.getAmountPkr()),
                        JournalService.Posting.credit("1200", receipt.getAmountPkr())
                ));
        return receipt;
    }
}
