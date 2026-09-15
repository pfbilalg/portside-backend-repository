package com.portside.trading.web.dto;

import java.util.List;

public record SalesInvoiceRequest(String customerCode, List<Line> lines) {
    public record Line(String itemCode, String containerCode, double qty, double ratePkr) {
    }
}
