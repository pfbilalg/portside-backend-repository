package com.portside.trading.web.dto;

import com.portside.trading.domain.PurchaseOrder;
import java.util.List;

public record PurchaseOrderDto(Long id, String code, String supplierCode, String supplierName, String orderDate,
                                String incoterm, String paymentTerms, String status, String containerCode,
                                List<PurchaseOrderLineDto> lines, double totalQty, double totalUsd, double totalPkr) {

    public record PurchaseOrderLineDto(Long lineId, String itemCode, String itemName, double qty,
                                        double fobUnitPriceUsd, double lineUsd) {
    }

    public static PurchaseOrderDto of(PurchaseOrder po, double usdRate) {
        List<PurchaseOrderLineDto> lines = po.getLines().stream()
                .map(l -> new PurchaseOrderLineDto(l.getId(), l.getItem().getCode(), l.getItem().getName(),
                        l.getQty(), l.getFobUnitPriceUsd(), l.getQty() * l.getFobUnitPriceUsd()))
                .toList();
        double qty = lines.stream().mapToDouble(PurchaseOrderLineDto::qty).sum();
        double usd = lines.stream().mapToDouble(PurchaseOrderLineDto::lineUsd).sum();
        return new PurchaseOrderDto(po.getId(), po.getCode(), po.getSupplier().getCode(), po.getSupplier().getName(),
                po.getOrderDate().toString(), po.getIncoterm(), po.getPaymentTerms(), po.getStatus().name(),
                po.getContainer() == null ? null : po.getContainer().getCode(), lines, qty, usd, usd * usdRate);
    }
}
