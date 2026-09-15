package com.portside.trading.web;

import com.portside.trading.domain.AllocationBasis;
import com.portside.trading.domain.Container;
import com.portside.trading.domain.InvoiceStatus;
import com.portside.trading.domain.Item;
import com.portside.trading.repo.ContainerRepository;
import com.portside.trading.repo.ItemRepository;
import com.portside.trading.repo.SalesInvoiceLineRepository;
import com.portside.trading.service.ContainerLotView;
import com.portside.trading.service.ContainerService;
import com.portside.trading.service.LandedCostService;
import com.portside.trading.web.dto.*;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/containers")
public class ContainerController {

    private final ContainerRepository containerRepository;
    private final ItemRepository itemRepository;
    private final LandedCostService landedCostService;
    private final ContainerService containerService;
    private final SalesInvoiceLineRepository invoiceLineRepository;

    public ContainerController(ContainerRepository containerRepository, ItemRepository itemRepository,
                                LandedCostService landedCostService, ContainerService containerService,
                                SalesInvoiceLineRepository invoiceLineRepository) {
        this.containerRepository = containerRepository;
        this.itemRepository = itemRepository;
        this.landedCostService = landedCostService;
        this.containerService = containerService;
        this.invoiceLineRepository = invoiceLineRepository;
    }

    @GetMapping
    @PreAuthorize("@accessService.canView('containers')")
    public List<ContainerSummaryDto> list() {
        return containerRepository.findAll().stream()
                .map(landedCostService::compute).map(ContainerSummaryDto::of).toList();
    }

    @GetMapping("/{code}")
    @PreAuthorize("@accessService.canView('containers')")
    @Transactional
    public ContainerDetailDto get(@PathVariable String code) {
        return detailOf(find(code));
    }

    @PutMapping("/{code}/basis")
    @PreAuthorize("@accessService.canEdit('containers')")
    @Transactional
    public ContainerDetailDto setBasis(@PathVariable String code, @RequestBody BasisRequest req) {
        Container c = find(code);
        if (req.basis() == null) containerService.cycleBasis(c);
        else containerService.setBasis(c, AllocationBasis.valueOf(req.basis()));
        return detailOf(c);
    }

    @PostMapping("/{code}/costs")
    @PreAuthorize("@accessService.canEdit('containers')")
    @Transactional
    public ContainerDetailDto addCost(@PathVariable String code) {
        return detailOf(containerService.addCostHead(find(code)));
    }

    @PutMapping("/{code}/costs/{costId}")
    @PreAuthorize("@accessService.canEdit('containers')")
    @Transactional
    public ContainerDetailDto updateCost(@PathVariable String code, @PathVariable Long costId, @RequestBody CostHeadRequest req) {
        AllocationBasis basis = req.basis() == null ? null : AllocationBasis.valueOf(req.basis());
        return detailOf(containerService.updateCostHead(find(code), costId, req.expenseHead(), req.amountPkr(), basis));
    }

    @DeleteMapping("/{code}/costs/{costId}")
    @PreAuthorize("@accessService.canEdit('containers')")
    @Transactional
    public ContainerDetailDto removeCost(@PathVariable String code, @PathVariable Long costId) {
        return detailOf(containerService.removeCostHead(find(code), costId));
    }

    @PostMapping("/{code}/lines")
    @PreAuthorize("@accessService.canEdit('containers')")
    @Transactional
    public ContainerDetailDto addLine(@PathVariable String code, @RequestBody ContainerLineRequest req) {
        Item item = itemRepository.findByCode(req.itemCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such item"));
        try {
            return detailOf(containerService.addLine(find(code), item, req.fobUnitPriceUsd() == null ? 10 : req.fobUnitPriceUsd()));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/{code}/lines/{lineId}")
    @PreAuthorize("@accessService.canEdit('containers')")
    @Transactional
    public ContainerDetailDto updateLine(@PathVariable String code, @PathVariable Long lineId, @RequestBody ContainerLineRequest req) {
        return detailOf(containerService.updateLine(find(code), lineId, req.qty(), req.fobUnitPriceUsd(), req.weightKg()));
    }

    @DeleteMapping("/{code}/lines/{lineId}")
    @PreAuthorize("@accessService.canEdit('containers')")
    @Transactional
    public ContainerDetailDto removeLine(@PathVariable String code, @PathVariable Long lineId) {
        return detailOf(containerService.removeLine(find(code), lineId));
    }

    private Container find(String code) {
        return containerRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such container"));
    }

    private ContainerDetailDto detailOf(Container container) {
        ContainerLotView view = landedCostService.compute(container);
        ContainerSummaryDto summary = ContainerSummaryDto.of(view);
        List<ContainerCostDto> costs = container.getCosts().stream().map(ContainerCostDto::of).toList();
        List<ContainerLineDto> lines = container.getLines().stream().map(line -> {
            var row = view.row(line.getItem().getId());
            return new ContainerLineDto(line.getId(), line.getItem().getId(), line.getItem().getCode(),
                    line.getItem().getName(), line.getItem().getUom(), line.getQty(), line.getFobUnitPriceUsd(),
                    line.getWeightKg(), row == null ? 0 : row.unitCost(), row == null ? 0 : row.sold(),
                    row == null ? 0 : row.onHand(), row == null ? 0 : row.stockValue());
        }).toList();
        double costPerUnit = view.qtyTotal() == 0 ? 0 : view.landedTotal() / view.qtyTotal();

        double revenue = 0, cogs = 0;
        for (var l : invoiceLineRepository.findByContainer_Id(container.getId())) {
            if (l.getInvoice().getStatus() != InvoiceStatus.POSTED) continue;
            revenue += l.getQty() * l.getRatePkr();
            cogs += l.getQty() * l.getUnitCostSnapshot();
        }

        return new ContainerDetailDto(summary, container.getCurrentBasis().name(), costs, lines,
                view.costTotal(), costPerUnit, revenue, cogs, revenue - cogs);
    }
}
