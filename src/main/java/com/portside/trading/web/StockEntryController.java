package com.portside.trading.web;

import com.portside.trading.repo.StockEntryRepository;
import com.portside.trading.service.StockEntryService;
import com.portside.trading.web.dto.StockEntryDto;
import com.portside.trading.web.dto.StockEntryRequest;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/stock-entries")
public class StockEntryController {

    private final StockEntryRepository stockEntryRepository;
    private final StockEntryService stockEntryService;

    public StockEntryController(StockEntryRepository stockEntryRepository, StockEntryService stockEntryService) {
        this.stockEntryRepository = stockEntryRepository;
        this.stockEntryService = stockEntryService;
    }

    @GetMapping
    @PreAuthorize("@accessService.canView('stockentries')")
    @Transactional
    public List<StockEntryDto> list() {
        return stockEntryRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(StockEntryDto::of).toList();
    }

    @PostMapping
    @PreAuthorize("@accessService.canEdit('stockentries')")
    @Transactional
    public StockEntryDto post(@RequestBody StockEntryRequest req) {
        try {
            return StockEntryDto.of(stockEntryService.post(req));
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
