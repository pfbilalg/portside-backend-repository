package com.portside.trading.web.dto;

import com.portside.trading.domain.StockEntry;
import java.util.List;

public record StockEntryDto(Long id, String code, String date, String type, String postedBy,
                             List<Line> lines, double netQty, double valueImpact) {

    public record Line(Long lineId, String itemCode, String itemName, String containerCode, double signedQty,
                        String reason, double unitCost, double valueImpact) {
    }

    public static StockEntryDto of(StockEntry e) {
        List<Line> lines = e.getLines().stream().map(l -> new Line(l.getId(), l.getItem().getCode(),
                l.getItem().getName(), l.getContainer().getCode(), l.getSignedQty(), l.getReason(),
                l.getUnitCostSnapshot(), l.getSignedQty() * l.getUnitCostSnapshot())).toList();
        double netQty = lines.stream().mapToDouble(Line::signedQty).sum();
        double value = lines.stream().mapToDouble(Line::valueImpact).sum();
        return new StockEntryDto(e.getId(), e.getCode(), e.getDate().toString(), e.getType().name(),
                e.getPostedBy(), lines, netQty, value);
    }
}
