package com.portside.trading.service;

import com.portside.trading.domain.Container;
import com.portside.trading.domain.Customer;
import com.portside.trading.domain.Item;
import com.portside.trading.domain.PriceListEntry;
import com.portside.trading.repo.PriceListEntryRepository;
import org.springframework.stereotype.Service;

/** Ports the prototype's priceRec()/tierPrice()/floorPrice() logic. */
@Service
public class PricingService {

    private final PriceListEntryRepository priceListRepository;
    private final LandedCostService landedCostService;

    public PricingService(PriceListEntryRepository priceListRepository, LandedCostService landedCostService) {
        this.priceListRepository = priceListRepository;
        this.landedCostService = landedCostService;
    }

    public PriceListEntry priceRec(Item item) {
        return priceListRepository.findByItem(item).orElseGet(() -> PriceListEntry.builder()
                .item(item).listPrice(1000).floorMarkupPct(8).tierBPct(2.5).tierCPct(5).tierDPct(3).build());
    }

    public double tierPrice(Item item, Customer customer) {
        PriceListEntry p = priceRec(item);
        double disc = customer == null ? 0 : customer.getStandingDiscountPct();
        return Math.round(p.getListPrice() * (1 - disc / 100) / 5) * 5.0;
    }

    public double floorPrice(Container container, Item item) {
        double unitCost = landedCostService.unitCost(container, item.getId());
        double floorMarkupPct = priceRec(item).getFloorMarkupPct();
        return unitCost * (1 + floorMarkupPct / 100);
    }
}
