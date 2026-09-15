package com.portside.trading.web.dto;

public record ContainerLineRequest(String itemCode, Double qty, Double fobUnitPriceUsd, Double weightKg) {
}
