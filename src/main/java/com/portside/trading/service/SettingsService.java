package com.portside.trading.service;

import com.portside.trading.domain.AppSetting;
import com.portside.trading.repo.AppSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final AppSettingRepository settingRepository;

    public SettingsService(AppSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public double usdRate() {
        return get("usdRate", 296.5);
    }

    public double taxRatePct() {
        return get("taxRatePct", 18);
    }

    public void set(String key, String value) {
        settingRepository.save(new AppSetting(key, value));
    }

    private double get(String key, double fallback) {
        return settingRepository.findById(key)
                .map(AppSetting::getSettingValue)
                .map(Double::parseDouble)
                .orElse(fallback);
    }
}
