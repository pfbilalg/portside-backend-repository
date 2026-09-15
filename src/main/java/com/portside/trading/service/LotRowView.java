package com.portside.trading.service;

public record LotRowView(
        Long itemId,
        String itemCode,
        String itemName,
        String uom,
        double qty,
        double fobPkr,
        double weightKg,
        double allocatedCost,
        double landedCost,
        double unitCost,
        double sold,
        double adjusted,
        double onHand,
        double stockValue
) {
}
