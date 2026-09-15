package com.portside.trading.web.dto;

import com.portside.trading.domain.PriceListEntry;

public record PriceListEntryDto(Long id, String itemCode, String itemName, double listPrice,
                                 double floorMarkupPct, double tierBPct, double tierCPct, double tierDPct) {
    public static PriceListEntryDto of(PriceListEntry p) {
        return new PriceListEntryDto(p.getId(), p.getItem().getCode(), p.getItem().getName(), p.getListPrice(),
                p.getFloorMarkupPct(), p.getTierBPct(), p.getTierCPct(), p.getTierDPct());
    }
}
