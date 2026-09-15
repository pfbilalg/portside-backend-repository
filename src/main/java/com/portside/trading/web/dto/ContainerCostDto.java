package com.portside.trading.web.dto;

import com.portside.trading.domain.ContainerCost;

public record ContainerCostDto(Long id, String expenseHead, double amountPkr, String basis) {
    public static ContainerCostDto of(ContainerCost c) {
        return new ContainerCostDto(c.getId(), c.getExpenseHead(), c.getAmountPkr(), c.getBasis().name());
    }
}
