package com.portside.trading.web.dto;

import com.portside.trading.domain.Supplier;
import jakarta.validation.constraints.NotBlank;

public record SupplierDto(Long id, @NotBlank String code, @NotBlank String name, @NotBlank String originPort,
                           @NotBlank String paymentTerms) {
    public static SupplierDto of(Supplier s) {
        return new SupplierDto(s.getId(), s.getCode(), s.getName(), s.getOriginPort(), s.getPaymentTerms());
    }
}
