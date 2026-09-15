package com.portside.trading.web.dto;

public record PriceListUpdateRequest(double listPrice, double floorMarkupPct, double tierBPct,
                                      double tierCPct, double tierDPct) {
}
