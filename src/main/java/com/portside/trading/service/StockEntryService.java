package com.portside.trading.service;

import com.portside.trading.domain.*;
import com.portside.trading.repo.ContainerRepository;
import com.portside.trading.repo.ItemRepository;
import com.portside.trading.repo.StockEntryRepository;
import com.portside.trading.security.CurrentUserService;
import com.portside.trading.web.dto.StockEntryRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ports the prototype's startStockEntry()/postStockEntry(): a signed quantity against a
 *  container lot, valued at that lot's current landed unit cost and posted immediately. */
@Service
public class StockEntryService {

    private final StockEntryRepository stockEntryRepository;
    private final ItemRepository itemRepository;
    private final ContainerRepository containerRepository;
    private final CodeGeneratorService codeGenerator;
    private final LandedCostService landedCostService;
    private final CurrentUserService currentUser;

    public StockEntryService(StockEntryRepository stockEntryRepository, ItemRepository itemRepository,
                              ContainerRepository containerRepository, CodeGeneratorService codeGenerator,
                              LandedCostService landedCostService, CurrentUserService currentUser) {
        this.stockEntryRepository = stockEntryRepository;
        this.itemRepository = itemRepository;
        this.containerRepository = containerRepository;
        this.codeGenerator = codeGenerator;
        this.landedCostService = landedCostService;
        this.currentUser = currentUser;
    }

    @Transactional
    public StockEntry post(StockEntryRequest req) {
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new IllegalStateException("Add at least one line before posting.");
        }
        StockEntry entry = StockEntry.builder()
                .code(codeGenerator.nextStockEntryCode())
                .date(LocalDate.now())
                .type(StockEntryType.valueOf(req.type()))
                .postedBy(currentUser.username())
                .lines(new ArrayList<>())
                .build();
        for (var l : req.lines()) {
            Item item = itemRepository.findByCode(l.itemCode())
                    .orElseThrow(() -> new IllegalArgumentException("No such item " + l.itemCode()));
            Container container = containerRepository.findByCode(l.containerCode())
                    .orElseThrow(() -> new IllegalArgumentException("No such container " + l.containerCode()));
            double unitCost = landedCostService.unitCost(container, item.getId());
            entry.getLines().add(StockEntryLine.builder().stockEntry(entry).item(item).container(container)
                    .signedQty(l.signedQty()).reason(l.reason()).unitCostSnapshot(unitCost).build());
        }
        return stockEntryRepository.save(entry);
    }
}
