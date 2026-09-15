package com.portside.trading.service;

import com.portside.trading.domain.InvoiceStatus;
import com.portside.trading.domain.SalesInvoice;
import org.springframework.stereotype.Service;

/** Shared net/tax/COGS/GP math for an invoice, reused by receipts, aging and reporting. */
@Service
public class InvoiceCalcService {

    public record Totals(double net, double tax, double gross, double cogs, double gp) {
    }

    private final LandedCostService landedCostService;
    private final SettingsService settingsService;

    public InvoiceCalcService(LandedCostService landedCostService, SettingsService settingsService) {
        this.landedCostService = landedCostService;
        this.settingsService = settingsService;
    }

    public Totals totals(SalesInvoice inv) {
        boolean posted = inv.getStatus() == InvoiceStatus.POSTED;
        double net = 0, cogs = 0;
        for (var line : inv.getLines()) {
            net += line.getQty() * line.getRatePkr();
            double unitCost = posted ? line.getUnitCostSnapshot()
                    : landedCostService.unitCost(line.getContainer(), line.getItem().getId());
            cogs += line.getQty() * unitCost;
        }
        double tax = net * (settingsService.taxRatePct() / 100);
        return new Totals(net, tax, net + tax, cogs, net - cogs);
    }
}
