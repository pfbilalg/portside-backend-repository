package com.portside.trading.service;

import com.portside.trading.domain.Container;
import com.portside.trading.repo.ContainerRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private final ContainerRepository containerRepository;
    private final LandedCostService landedCostService;

    public StockService(ContainerRepository containerRepository, LandedCostService landedCostService) {
        this.containerRepository = containerRepository;
        this.landedCostService = landedCostService;
    }

    /** every container's costed worksheet, whether received or still in transit */
    public List<ContainerLotView> allLots() {
        return containerRepository.findAll().stream().map(landedCostService::compute).toList();
    }

    /** only lots that have actually reached the warehouse and carry sellable stock */
    public List<ContainerLotView> liveLots() {
        return allLots().stream().filter(ContainerLotView::live).toList();
    }

    public double totalStockValue() {
        return liveLots().stream()
                .flatMap(l -> l.rows().stream())
                .mapToDouble(LotRowView::stockValue)
                .sum();
    }

    public double totalInTransitValue() {
        return allLots().stream().filter(l -> !l.live())
                .mapToDouble(ContainerLotView::landedTotal)
                .sum();
    }

    public double onHand(Container container, Long itemId) {
        return landedCostService.onHand(container, itemId);
    }
}
