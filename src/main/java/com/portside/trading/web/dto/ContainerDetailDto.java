package com.portside.trading.web.dto;

import java.util.List;

public record ContainerDetailDto(ContainerSummaryDto summary, String basis, List<ContainerCostDto> costs,
                                  List<ContainerLineDto> lines, double costTotal, double costPerUnit,
                                  double revenue, double cogs, double grossProfit) {
}
