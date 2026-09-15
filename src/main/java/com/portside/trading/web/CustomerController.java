package com.portside.trading.web;

import com.portside.trading.domain.Customer;
import com.portside.trading.repo.CustomerRepository;
import com.portside.trading.web.dto.CustomerDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    @PreAuthorize("@accessService.canView('customers')")
    public List<CustomerDto> list() {
        return customerRepository.findAll().stream().map(CustomerDto::of).toList();
    }

    @PostMapping
    @PreAuthorize("@accessService.canEdit('customers')")
    public CustomerDto create(@Valid @RequestBody CustomerDto dto) {
        Customer c = Customer.builder().code(dto.code()).name(dto.name()).city(dto.city())
                .paymentTerms(dto.paymentTerms()).creditLimit(dto.creditLimit())
                .standingDiscountPct(dto.standingDiscountPct()).salesman(dto.salesman()).build();
        return CustomerDto.of(customerRepository.save(c));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessService.canEdit('customers')")
    public CustomerDto update(@PathVariable Long id, @Valid @RequestBody CustomerDto dto) {
        Customer c = customerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.setCode(dto.code());
        c.setName(dto.name());
        c.setCity(dto.city());
        c.setPaymentTerms(dto.paymentTerms());
        c.setCreditLimit(dto.creditLimit());
        c.setStandingDiscountPct(dto.standingDiscountPct());
        c.setSalesman(dto.salesman());
        return CustomerDto.of(customerRepository.save(c));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessService.isOwner()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
