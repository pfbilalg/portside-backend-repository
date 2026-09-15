package com.portside.trading.web;

import com.portside.trading.domain.PurchaseOrder;
import com.portside.trading.repo.PurchaseOrderRepository;
import com.portside.trading.service.PurchaseOrderService;
import com.portside.trading.service.SettingsService;
import com.portside.trading.web.dto.ContainerSummaryDto;
import com.portside.trading.web.dto.PurchaseOrderDto;
import com.portside.trading.web.dto.PurchaseOrderRequest;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderService poService;
    private final SettingsService settingsService;
    private final com.portside.trading.service.LandedCostService landedCostService;

    public PurchaseOrderController(PurchaseOrderRepository poRepository, PurchaseOrderService poService,
                                    SettingsService settingsService, com.portside.trading.service.LandedCostService landedCostService) {
        this.poRepository = poRepository;
        this.poService = poService;
        this.settingsService = settingsService;
        this.landedCostService = landedCostService;
    }

    @GetMapping
    @PreAuthorize("@accessService.canView('purchases')")
    @Transactional
    public List<PurchaseOrderDto> list() {
        double rate = settingsService.usdRate();
        return poRepository.findAll().stream().map(po -> PurchaseOrderDto.of(po, rate)).toList();
    }

    @GetMapping("/{code}")
    @PreAuthorize("@accessService.canView('purchases')")
    @Transactional
    public PurchaseOrderDto get(@PathVariable String code) {
        return PurchaseOrderDto.of(find(code), settingsService.usdRate());
    }

    @PostMapping
    @PreAuthorize("@accessService.canEdit('purchases')")
    @Transactional
    public PurchaseOrderDto create(@RequestBody PurchaseOrderRequest req) {
        return PurchaseOrderDto.of(poService.create(req), settingsService.usdRate());
    }

    @PutMapping("/{code}")
    @PreAuthorize("@accessService.canEdit('purchases')")
    @Transactional
    public PurchaseOrderDto update(@PathVariable String code, @RequestBody PurchaseOrderRequest req) {
        return PurchaseOrderDto.of(poService.update(find(code), req), settingsService.usdRate());
    }

    @PostMapping("/{code}/receive")
    @PreAuthorize("@accessService.canEdit('purchases')")
    @Transactional
    public ContainerSummaryDto receive(@PathVariable String code) {
        try {
            var container = poService.receive(find(code));
            return ContainerSummaryDto.of(landedCostService.compute(container));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("@accessService.isOwner()")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable String code) {
        poRepository.delete(find(code));
        return ResponseEntity.noContent().build();
    }

    private PurchaseOrder find(String code) {
        return poRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such purchase order"));
    }
}
