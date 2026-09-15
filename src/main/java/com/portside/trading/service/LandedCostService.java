package com.portside.trading.service;

import com.portside.trading.domain.AllocationBasis;
import com.portside.trading.domain.Container;
import com.portside.trading.domain.ContainerCost;
import com.portside.trading.domain.ContainerLine;
import com.portside.trading.repo.SalesInvoiceLineRepository;
import com.portside.trading.repo.StockEntryLineRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Ports the prototype's lots()/soldQty()/adjQty()/unitCost() logic
 * (Portside Container Trading System.dc.html, lines ~928-1010): allocates each container's
 * expense heads across its lines by value/weight/quantity share, then derives on-hand qty as
 * received + stock-entry adjustments - sold.
 */
@Service
public class LandedCostService {

    private final SettingsService settingsService;
    private final SalesInvoiceLineRepository invoiceLineRepository;
    private final StockEntryLineRepository stockEntryLineRepository;

    public LandedCostService(SettingsService settingsService,
                              SalesInvoiceLineRepository invoiceLineRepository,
                              StockEntryLineRepository stockEntryLineRepository) {
        this.settingsService = settingsService;
        this.invoiceLineRepository = invoiceLineRepository;
        this.stockEntryLineRepository = stockEntryLineRepository;
    }

    public ContainerLotView compute(Container container) {
        double usdRate = settingsService.usdRate();
        AllocationBasis overallBasis = container.getCurrentBasis();

        record Calc(ContainerLine line, double fobPkr) {}
        List<Calc> calcs = new ArrayList<>();
        double totV = 0, totW = 0, totQ = 0;
        for (ContainerLine line : container.getLines()) {
            double fob = line.getQty() * line.getFobUnitPriceUsd() * usdRate;
            calcs.add(new Calc(line, fob));
            totV += fob;
            totW += line.getWeightKg();
            totQ += line.getQty();
        }

        double costTotal = 0;
        for (ContainerCost cost : container.getCosts()) costTotal += cost.getAmountPkr();

        List<LotRowView> rows = new ArrayList<>();
        for (Calc c : calcs) {
            double alloc = 0;
            for (ContainerCost cost : container.getCosts()) {
                AllocationBasis basis = cost.getBasis() == AllocationBasis.OVERALL ? overallBasis : cost.getBasis();
                double share = switch (basis) {
                    case VALUE -> totV == 0 ? 0 : c.fobPkr / totV;
                    case WEIGHT -> totW == 0 ? 0 : c.line.getWeightKg() / totW;
                    case QUANTITY -> totQ == 0 ? 0 : c.line.getQty() / totQ;
                    case OVERALL -> 0; // unreachable: overallBasis is never itself OVERALL
                };
                alloc += cost.getAmountPkr() * share;
            }
            double landed = c.fobPkr + alloc;
            double unit = c.line.getQty() == 0 ? 0 : landed / c.line.getQty();
            Long itemId = c.line.getItem().getId();
            Long containerId = container.getId();
            double sold = soldQty(containerId, itemId);
            double adj = adjQty(containerId, itemId);
            double onHand = c.line.getQty() + adj - sold;
            rows.add(new LotRowView(
                    itemId, c.line.getItem().getCode(), c.line.getItem().getName(), c.line.getItem().getUom(),
                    c.line.getQty(), c.fobPkr, c.line.getWeightKg(), alloc, landed, unit,
                    sold, adj, onHand, onHand * unit));
        }

        return new ContainerLotView(container, totV, totW, totQ, costTotal, totV + costTotal,
                container.isReceived(), rows);
    }

    public double soldQty(Long containerId, Long itemId) {
        return invoiceLineRepository.findByContainer_IdAndItem_Id(containerId, itemId).stream()
                .filter(l -> l.getInvoice().getStatus() == com.portside.trading.domain.InvoiceStatus.POSTED)
                .mapToDouble(l -> l.getQty())
                .sum();
    }

    public double adjQty(Long containerId, Long itemId) {
        return stockEntryLineRepository.findByContainer_IdAndItem_Id(containerId, itemId).stream()
                .mapToDouble(l -> l.getSignedQty())
                .sum();
    }

    public double unitCost(Container container, Long itemId) {
        LotRowView row = compute(container).row(itemId);
        return row == null ? 0 : row.unitCost();
    }

    public double onHand(Container container, Long itemId) {
        if (!container.isReceived()) return 0;
        LotRowView row = compute(container).row(itemId);
        return row == null ? 0 : row.onHand();
    }
}
