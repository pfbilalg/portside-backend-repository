package com.portside.trading.web.dto;

import com.portside.trading.domain.Receipt;
import java.util.List;

public record ReceiptDto(Long id, String code, String date, String customerCode, String customerName,
                          double amountPkr, String mode, List<Allocation> allocations) {

    public record Allocation(String invoiceCode, double amountApplied) {
    }

    public static ReceiptDto of(Receipt r) {
        List<Allocation> allocs = r.getAllocations().stream()
                .map(a -> new Allocation(a.getInvoice().getCode(), a.getAmountApplied())).toList();
        return new ReceiptDto(r.getId(), r.getCode(), r.getDate().toString(), r.getCustomer().getCode(),
                r.getCustomer().getName(), r.getAmountPkr(), r.getMode(), allocs);
    }
}
