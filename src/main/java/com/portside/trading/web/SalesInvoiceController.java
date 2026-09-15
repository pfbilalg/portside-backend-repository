package com.portside.trading.web;

import com.portside.trading.domain.InvoiceStatus;
import com.portside.trading.domain.SalesInvoice;
import com.portside.trading.repo.ContainerRepository;
import com.portside.trading.repo.ItemRepository;
import com.portside.trading.repo.SalesInvoiceRepository;
import com.portside.trading.service.LandedCostService;
import com.portside.trading.service.SalesInvoiceService;
import com.portside.trading.service.SettingsService;
import com.portside.trading.web.dto.SalesInvoiceDto;
import com.portside.trading.web.dto.SalesInvoiceRequest;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sales-invoices")
public class SalesInvoiceController {

    private final SalesInvoiceRepository invoiceRepository;
    private final SalesInvoiceService invoiceService;
    private final SettingsService settingsService;
    private final LandedCostService landedCostService;
    private final ContainerRepository containerRepository;
    private final ItemRepository itemRepository;

    public SalesInvoiceController(SalesInvoiceRepository invoiceRepository, SalesInvoiceService invoiceService,
                                   SettingsService settingsService, LandedCostService landedCostService,
                                   ContainerRepository containerRepository, ItemRepository itemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.settingsService = settingsService;
        this.landedCostService = landedCostService;
        this.containerRepository = containerRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    @PreAuthorize("@accessService.canView('sales')")
    @Transactional
    public List<SalesInvoiceDto> list() {
        double taxRate = settingsService.taxRatePct();
        return invoiceRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(inv -> SalesInvoiceDto.of(inv, taxRate, this::liveUnitCost))
                .toList();
    }

    @GetMapping("/{code}")
    @PreAuthorize("@accessService.canView('sales')")
    @Transactional
    public SalesInvoiceDto get(@PathVariable String code) {
        return SalesInvoiceDto.of(find(code), settingsService.taxRatePct(), this::liveUnitCost);
    }

    @PostMapping
    @PreAuthorize("@accessService.canEdit('sales')")
    @Transactional
    public SalesInvoiceDto create(@RequestBody SalesInvoiceRequest req) {
        return SalesInvoiceDto.of(invoiceService.create(req), settingsService.taxRatePct(), this::liveUnitCost);
    }

    @PutMapping("/{code}")
    @PreAuthorize("@accessService.canEdit('sales')")
    @Transactional
    public SalesInvoiceDto update(@PathVariable String code, @RequestBody SalesInvoiceRequest req) {
        try {
            return SalesInvoiceDto.of(invoiceService.update(find(code), req), settingsService.taxRatePct(), this::liveUnitCost);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{code}/post")
    @PreAuthorize("@accessService.canEdit('sales')")
    @Transactional
    public ResponseEntity<Map<String, Object>> post(@PathVariable String code) {
        try {
            boolean posted = invoiceService.post(find(code));
            var dto = SalesInvoiceDto.of(find(code), settingsService.taxRatePct(), this::liveUnitCost);
            return ResponseEntity.ok(Map.of("posted", posted, "invoice", dto));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{code}/approve")
    @PreAuthorize("@accessService.isOwner()")
    @Transactional
    public SalesInvoiceDto approve(@PathVariable String code) {
        try {
            invoiceService.approve(find(code));
            return SalesInvoiceDto.of(find(code), settingsService.taxRatePct(), this::liveUnitCost);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("@accessService.isOwner()")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable String code) {
        SalesInvoice inv = find(code);
        if (inv.getStatus() == InvoiceStatus.POSTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Posted invoices can't be deleted.");
        }
        invoiceRepository.delete(inv);
        return ResponseEntity.noContent().build();
    }

    private double liveUnitCost(String containerCode, String itemCode) {
        var container = containerRepository.findByCode(containerCode).orElseThrow();
        Long itemId = itemRepository.findByCode(itemCode).orElseThrow().getId();
        return landedCostService.unitCost(container, itemId);
    }

    private SalesInvoice find(String code) {
        return invoiceRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such invoice"));
    }
}
