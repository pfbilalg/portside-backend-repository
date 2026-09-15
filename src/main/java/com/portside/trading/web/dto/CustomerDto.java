package com.portside.trading.web.dto;

import com.portside.trading.domain.Customer;
import jakarta.validation.constraints.NotBlank;

public record CustomerDto(Long id, @NotBlank String code, @NotBlank String name, @NotBlank String city,
                           @NotBlank String paymentTerms, double creditLimit, double standingDiscountPct,
                           @NotBlank String salesman) {
    public static CustomerDto of(Customer c) {
        return new CustomerDto(c.getId(), c.getCode(), c.getName(), c.getCity(), c.getPaymentTerms(),
                c.getCreditLimit(), c.getStandingDiscountPct(), c.getSalesman());
    }
}
