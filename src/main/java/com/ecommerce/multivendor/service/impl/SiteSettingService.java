package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.dto.request.SiteSettingUpdateRequest;
import com.ecommerce.multivendor.dto.response.ApiResponse;
import com.ecommerce.multivendor.dto.response.SiteSettingResponse;
import com.ecommerce.multivendor.entity.SiteSetting;
import com.ecommerce.multivendor.repository.SiteSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SiteSettingService {

    private final SiteSettingRepository siteSettingRepository;

    @Transactional(readOnly = true)
    public ApiResponse<SiteSettingResponse> getSettings() {
        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);

        return ApiResponse.success("Settings retrieved successfully", mapToResponse(setting));
    }

    @Transactional
    public ApiResponse<SiteSettingResponse> updateSettings(SiteSettingUpdateRequest request) {
        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);

        setting.setSiteName(request.getSiteName());
        setting.setContactEmail(request.getContactEmail());
        setting.setCurrencySymbol(request.getCurrencySymbol());
        setting.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : request.getCurrencySymbol());
        setting.setPhoneNumber(request.getPhoneNumber());
        setting.setAddress(request.getAddress());
        setting.setLogoUrl(request.getLogoUrl());
        setting.setFaviconUrl(request.getFaviconUrl());
        setting.setMetaDescription(request.getMetaDescription());
        setting.setTaxRate(request.getTaxRate() != null ? request.getTaxRate() : 0);
        setting.setShippingFee(request.getShippingFee() != null ? request.getShippingFee() : 0);
        setting.setFreeShippingThreshold(request.getFreeShippingThreshold());

        SiteSetting updated = siteSettingRepository.save(setting);
        return ApiResponse.success("Settings updated successfully", mapToResponse(updated));
    }

    private SiteSetting createDefaultSettings() {
        return siteSettingRepository.save(SiteSetting.builder().build());
    }

    private SiteSettingResponse mapToResponse(SiteSetting setting) {
        return SiteSettingResponse.builder()
                .id(setting.getId())
                .siteName(setting.getSiteName())
                .contactEmail(setting.getContactEmail())
                .currencySymbol(setting.getCurrencySymbol())
                .currencyCode(setting.getCurrencyCode())
                .phoneNumber(setting.getPhoneNumber())
                .address(setting.getAddress())
                .logoUrl(setting.getLogoUrl())
                .faviconUrl(setting.getFaviconUrl())
                .metaDescription(setting.getMetaDescription())
                .taxRate(setting.getTaxRate())
                .shippingFee(setting.getShippingFee())
                .freeShippingThreshold(setting.getFreeShippingThreshold())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}