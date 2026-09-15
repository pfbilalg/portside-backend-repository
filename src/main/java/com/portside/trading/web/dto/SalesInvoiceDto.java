package com.portside.trading.web.dto;

import com.portside.trading.domain.SalesInvoice;
import java.util.List;

public record SalesInvoiceDto(Long id, String code, String date, String customerCode, String customerName,
                               String status, List<Line> lines, double net, double tax, double gross,
                               double cogs, double grossProfit) {

    public record Line(Long lineId, String itemCode, String itemName, String containerCode, double qty,
                        double ratePkr, double unitCost, double lineTotal, double lineGp) {
    }

    public static SalesInvoiceDto of(SalesInvoice inv, double taxRatePct,
                                      java.util.function.BiFunction<String, String, Double> liveUnitCost) {
        List<Line> lines = inv.getLines().stream().map(l -> {
            boolean posted = inv.getStatus() == com.portside.trading.domain.InvoiceStatus.POSTED;
            double unitCost = posted ? l.getUnitCostSnapshot()
                    : liveUnitCost.apply(l.getContainer().getCode(), l.getItem().getCode());
            double total = l.getQty() * l.getRatePkr();
            return new Line(l.getId(), l.getItem().getCode(), l.getItem().getName(), l.getContainer().getCode(),
                    l.getQty(), l.getRatePkr(), unitCost, total, total - l.getQty() * unitCost);
        }).toList();
        double net = lines.stream().mapToDouble(Line::lineTotal).sum();
        double cogs = lines.stream().mapToDouble(l -> l.qty() * l.unitCost()).sum();
        double tax = net * (taxRatePct / 100);
        return new SalesInvoiceDto(inv.getId(), inv.getCode(), inv.getDate().toString(), inv.getCustomer().getCode(),
                inv.getCustomer().getName(), inv.getStatus().name(), lines, net, tax, net + tax, cogs, net - cogs);
    }
}
