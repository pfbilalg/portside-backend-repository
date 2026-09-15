package com.portside.trading.web;

import com.portside.trading.domain.Supplier;
import com.portside.trading.repo.SupplierRepository;
import com.portside.trading.web.dto.SupplierDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierRepository supplierRepository;

    public SupplierController(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @GetMapping
    @PreAuthorize("@accessService.canView('suppliers')")
    public List<SupplierDto> list() {
        return supplierRepository.findAll().stream().map(SupplierDto::of).toList();
    }

    @PostMapping
    @PreAuthorize("@accessService.canEdit('suppliers')")
    public SupplierDto create(@Valid @RequestBody SupplierDto dto) {
        Supplier s = Supplier.builder().code(dto.code()).name(dto.name()).originPort(dto.originPort())
                .paymentTerms(dto.paymentTerms()).build();
        return SupplierDto.of(supplierRepository.save(s));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessService.canEdit('suppliers')")
    public SupplierDto update(@PathVariable Long id, @Valid @RequestBody SupplierDto dto) {
        Supplier s = supplierRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setCode(dto.code());
        s.setName(dto.name());
        s.setOriginPort(dto.originPort());
        s.setPaymentTerms(dto.paymentTerms());
        return SupplierDto.of(supplierRepository.save(s));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessService.isOwner()")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
