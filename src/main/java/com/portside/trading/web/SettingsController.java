package com.portside.trading.web;

import com.portside.trading.service.SettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    public record SettingsDto(double usdRate, double taxRatePct) {
    }

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsDto get() {
        return new SettingsDto(settingsService.usdRate(), settingsService.taxRatePct());
    }

    @PutMapping
    @PreAuthorize("@accessService.isOwner()")
    public SettingsDto update(@RequestBody SettingsDto dto) {
        settingsService.set("usdRate", String.valueOf(dto.usdRate()));
        settingsService.set("taxRatePct", String.valueOf(dto.taxRatePct()));
        return get();
    }
}
