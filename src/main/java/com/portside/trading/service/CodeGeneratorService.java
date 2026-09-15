package com.portside.trading.service;

import com.portside.trading.repo.*;
import org.springframework.stereotype.Service;

/** Generates the next human-readable business code per document type, continuing the seed sequence. */
@Service
public class CodeGeneratorService {

    private final PurchaseOrderRepository poRepository;
    private final ContainerRepository containerRepository;
    private final SalesInvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final StockEntryRepository stockEntryRepository;
    private final JournalEntryRepository journalEntryRepository;

    public CodeGeneratorService(PurchaseOrderRepository poRepository, ContainerRepository containerRepository,
                                 SalesInvoiceRepository invoiceRepository, ReceiptRepository receiptRepository,
                                 StockEntryRepository stockEntryRepository, JournalEntryRepository journalEntryRepository) {
        this.poRepository = poRepository;
        this.containerRepository = containerRepository;
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.stockEntryRepository = stockEntryRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    public synchronized String nextPurchaseOrderCode() {
        return "PO-" + (2600 + poRepository.count() + 1);
    }

    public synchronized String nextContainerCode() {
        return "PSC-" + (2600 + containerRepository.count() + 1);
    }

    public synchronized String nextInvoiceCode() {
        return String.format("INV-%04d", 30 + invoiceRepository.count() + 1);
    }

    public synchronized String nextReceiptCode() {
        return String.format("RV-%04d", 20 + receiptRepository.count() + 1);
    }

    public synchronized String nextStockEntryCode() {
        return String.format("SE-%04d", stockEntryRepository.count() + 1);
    }

    public synchronized String nextVoucherNo(String prefix) {
        return prefix + "-" + String.format("%05d", journalEntryRepository.count() + 1);
    }
}
