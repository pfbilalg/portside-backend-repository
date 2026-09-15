package com.portside.trading.service;

import com.portside.trading.domain.Container;
import java.util.List;

public record ContainerLotView(
        Container container,
        double fobTotal,
        double weightTotal,
        double qtyTotal,
        double costTotal,
        double landedTotal,
        boolean live,
        List<LotRowView> rows
) {
    public LotRowView row(Long itemId) {
        return rows.stream().filter(r -> r.itemId().equals(itemId)).findFirst().orElse(null);
    }
}
