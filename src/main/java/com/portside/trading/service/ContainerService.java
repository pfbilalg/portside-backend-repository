package com.portside.trading.service;

import com.portside.trading.domain.AllocationBasis;
import com.portside.trading.domain.Container;
import com.portside.trading.domain.ContainerCost;
import com.portside.trading.domain.ContainerLine;
import com.portside.trading.domain.Item;
import com.portside.trading.repo.ContainerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ports the prototype's cycleBasis()/addCostHead()/removeCost()/setCost()/addLotItem()/setLotLine()/removeLotLine(). */
@Service
public class ContainerService {

    private static final List<AllocationBasis> BASIS_CYCLE =
            List.of(AllocationBasis.VALUE, AllocationBasis.WEIGHT, AllocationBasis.QUANTITY);

    private final ContainerRepository containerRepository;

    public ContainerService(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }

    @Transactional
    public Container cycleBasis(Container container) {
        int idx = BASIS_CYCLE.indexOf(container.getCurrentBasis());
        container.setCurrentBasis(BASIS_CYCLE.get((idx + 1) % BASIS_CYCLE.size()));
        return containerRepository.save(container);
    }

    @Transactional
    public Container setBasis(Container container, AllocationBasis basis) {
        container.setCurrentBasis(basis);
        return containerRepository.save(container);
    }

    @Transactional
    public Container addCostHead(Container container) {
        container.getCosts().add(ContainerCost.builder()
                .container(container).expenseHead("New expense head").amountPkr(0).basis(AllocationBasis.OVERALL).build());
        return containerRepository.save(container);
    }

    @Transactional
    public Container updateCostHead(Container container, Long costId, String head, Double amount, AllocationBasis basis) {
        ContainerCost cost = container.getCosts().stream().filter(c -> c.getId().equals(costId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such cost head on this container"));
        if (head != null) cost.setExpenseHead(head);
        if (amount != null) cost.setAmountPkr(Math.max(0, amount));
        if (basis != null) cost.setBasis(basis);
        return containerRepository.save(container);
    }

    @Transactional
    public Container removeCostHead(Container container, Long costId) {
        container.getCosts().removeIf(c -> c.getId().equals(costId));
        return containerRepository.save(container);
    }

    @Transactional
    public Container addLine(Container container, Item item, double fobUnitPriceUsd) {
        boolean exists = container.getLines().stream().anyMatch(l -> l.getItem().getId().equals(item.getId()));
        if (exists) throw new IllegalStateException(item.getName() + " is already on this lot — edit its quantity instead.");
        container.getLines().add(ContainerLine.builder()
                .container(container).item(item).qty(100).fobUnitPriceUsd(fobUnitPriceUsd > 0 ? fobUnitPriceUsd : 10).weightKg(300).build());
        return containerRepository.save(container);
    }

    @Transactional
    public Container updateLine(Container container, Long lineId, Double qty, Double fob, Double weight) {
        ContainerLine line = container.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such line on this container"));
        if (qty != null) line.setQty(Math.max(0, qty));
        if (fob != null) line.setFobUnitPriceUsd(Math.max(0, fob));
        if (weight != null) line.setWeightKg(Math.max(0, weight));
        return containerRepository.save(container);
    }

    @Transactional
    public Container removeLine(Container container, Long lineId) {
        container.getLines().removeIf(l -> l.getId().equals(lineId));
        return containerRepository.save(container);
    }
}
