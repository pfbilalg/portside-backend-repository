package com.portside.trading.web;

import com.portside.trading.domain.Item;
import com.portside.trading.domain.PriceListEntry;
import com.portside.trading.repo.ItemRepository;
import com.portside.trading.repo.PriceListEntryRepository;
import com.portside.trading.service.PricingService;
import com.portside.trading.web.dto.PriceListEntryDto;
import com.portside.trading.web.dto.PriceListUpdateRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/price-list")
public class PriceListController {

    private final ItemRepository itemRepository;
    private final PriceListEntryRepository priceListRepository;
    private final PricingService pricingService;

    public PriceListController(ItemRepository itemRepository, PriceListEntryRepository priceListRepository,
                                PricingService pricingService) {
        this.itemRepository = itemRepository;
        this.priceListRepository = priceListRepository;
        this.pricingService = pricingService;
    }

    @GetMapping
    @PreAuthorize("@accessService.canView('pricing')")
    public List<PriceListEntryDto> list() {
        // ensures every item has a (possibly cost-derived default) price row, mirroring the prototype's priceRec()
        return itemRepository.findAll().stream()
                .map(item -> priceListRepository.findByItem(item).orElseGet(() -> pricingService.priceRec(item)))
                .map(PriceListEntryDto::of)
                .toList();
    }

    @PutMapping("/{itemCode}")
    @PreAuthorize("@accessService.canEdit('pricing')")
    public PriceListEntryDto upsert(@PathVariable String itemCode, @RequestBody PriceListUpdateRequest req) {
        Item item = itemRepository.findByCode(itemCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PriceListEntry entry = priceListRepository.findByItem(item).orElseGet(() -> PriceListEntry.builder().item(item).build());
        entry.setListPrice(req.listPrice());
        entry.setFloorMarkupPct(req.floorMarkupPct());
        entry.setTierBPct(req.tierBPct());
        entry.setTierCPct(req.tierCPct());
        entry.setTierDPct(req.tierDPct());
        return PriceListEntryDto.of(priceListRepository.save(entry));
    }
}
