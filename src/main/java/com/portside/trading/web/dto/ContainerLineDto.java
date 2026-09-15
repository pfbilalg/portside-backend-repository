package com.portside.trading.web.dto;

public record ContainerLineDto(Long lineId, Long itemId, String itemCode, String itemName, String uom,
                                double qty, double fobUnitPriceUsd, double weightKg,
                                double unitCost, double sold, double onHand, double stockValue) {
}
