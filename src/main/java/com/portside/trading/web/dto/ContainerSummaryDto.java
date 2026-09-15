package com.portside.trading.web.dto;

import com.portside.trading.service.ContainerLotView;

public record ContainerSummaryDto(Long id, String code, String containerNo, String supplierCode, String supplierName,
                                   String blNumber, String gdNumber, String size, String eta, String receivedDate,
                                   String status, double fobTotal, double costTotal, double landedTotal,
                                   double stockValue, boolean live) {
    public static ContainerSummaryDto of(ContainerLotView v) {
        var c = v.container();
        double stockValue = v.rows().stream().mapToDouble(r -> r.stockValue()).sum();
        return new ContainerSummaryDto(c.getId(), c.getCode(), c.getContainerNo(), c.getSupplier().getCode(),
                c.getSupplier().getName(), c.getBlNumber(), c.getGdNumber(), c.getSize(),
                c.getEta() == null ? null : c.getEta().toString(),
                c.getReceivedDate() == null ? null : c.getReceivedDate().toString(),
                c.getStatus().name(), v.fobTotal(), v.costTotal(), v.landedTotal(), stockValue, v.live());
    }
}
