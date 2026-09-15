package com.portside.trading.web.dto;

public record ReceiptRequest(String customerCode, double amountPkr, String mode) {
}
