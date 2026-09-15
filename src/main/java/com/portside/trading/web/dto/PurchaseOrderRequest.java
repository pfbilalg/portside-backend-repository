package com.portside.trading.web.dto;

import java.util.List;

public record PurchaseOrderRequest(String supplierCode, String orderDate, String incoterm, String paymentTerms,
                                    String status, List<Line> lines) {
    public record Line(String itemCode, double qty, double fobUnitPriceUsd) {
    }
}
