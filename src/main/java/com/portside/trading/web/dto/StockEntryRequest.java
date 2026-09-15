package com.portside.trading.web.dto;

import java.util.List;

public record StockEntryRequest(String type, List<Line> lines) {
    public record Line(String itemCode, String containerCode, double signedQty, String reason) {
    }
}
