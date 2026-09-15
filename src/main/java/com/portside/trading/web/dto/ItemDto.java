package com.portside.trading.web.dto;

import com.portside.trading.domain.Item;
import jakarta.validation.constraints.NotBlank;

public record ItemDto(Long id, @NotBlank String code, @NotBlank String name, @NotBlank String uom,
                       @NotBlank String category, double reorderLevel) {
    public static ItemDto of(Item i) {
        return new ItemDto(i.getId(), i.getCode(), i.getName(), i.getUom(), i.getCategory(), i.getReorderLevel());
    }
}
