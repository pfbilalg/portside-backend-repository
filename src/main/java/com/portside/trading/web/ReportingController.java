package com.portside.trading.web;

import com.portside.trading.service.ReportingService;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@accessService.canView('dash')")
    @Transactional
    public ReportingService.DashboardSummary dashboard() {
        return reportingService.dashboard();
    }

    @GetMapping("/trial-balance")
    @PreAuthorize("@accessService.canView('tb')")
    @Transactional
    public ReportingService.TrialBalance trialBalance() {
        return reportingService.trialBalance();
    }

    @GetMapping("/general-ledger")
    @PreAuthorize("@accessService.canView('gl')")
    @Transactional
    public List<ReportingService.GlLine> generalLedger(@RequestParam(defaultValue = "100") int limit) {
        return reportingService.generalLedger(limit);
    }

    @GetMapping("/container-profitability")
    @PreAuthorize("@accessService.canView('profit')")
    @Transactional
    public List<ReportingService.ContainerProfitability> containerProfitability() {
        return reportingService.containerProfitability();
    }

    @GetMapping("/receivables-aging")
    @PreAuthorize("@accessService.canView('receivables')")
    @Transactional
    public List<ReportingService.CustomerReceivable> receivablesAging() {
        return reportingService.receivablesAging();
    }

    @GetMapping("/aging-buckets")
    @PreAuthorize("@accessService.canView('receivables')")
    @Transactional
    public ReportingService.AgingBuckets agingBuckets() {
        return reportingService.agingBuckets();
    }

    @GetMapping("/stock")
    @PreAuthorize("@accessService.canView('stock')")
    @Transactional
    public List<ReportingService.ItemStock> stock() {
        return reportingService.stockOnHand();
    }
}
