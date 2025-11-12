package com.cloudkitchen.rbac.service.impl;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.domain.entity.MerchantWebsiteConfig;
import com.cloudkitchen.rbac.dto.website.WebsiteConfigRequest;
import com.cloudkitchen.rbac.dto.website.WebsiteConfigResponse;
import com.cloudkitchen.rbac.exception.BusinessExceptions.MerchantNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.ValidationException;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.repository.MerchantWebsiteConfigRepository;
import com.cloudkitchen.rbac.service.CloudStorageService;
import com.cloudkitchen.rbac.service.WebsiteConfigService;

@Service
public class WebsiteConfigServiceImpl implements WebsiteConfigService {
    
    private static final Logger log = LoggerFactory.getLogger(WebsiteConfigServiceImpl.class);
    
    private final MerchantWebsiteConfigRepository configRepository;
    private final MerchantRepository merchantRepository;
    private final CloudStorageService cloudStorageService;

    public WebsiteConfigServiceImpl(MerchantWebsiteConfigRepository configRepository,
                                   MerchantRepository merchantRepository,
                                   CloudStorageService cloudStorageService) {
        this.configRepository = configRepository;
        this.merchantRepository = merchantRepository;
        this.cloudStorageService = cloudStorageService;
    }

    @Override
    @Transactional
    public WebsiteConfigResponse saveConfiguration(Integer merchantId, WebsiteConfigRequest request) {
        log.info("Saving website configuration for merchant: {}", merchantId);
        
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new MerchantNotFoundException("Merchant not found with ID: " + merchantId));
        
        validateWebsiteAddress(request.getWebsiteAddress(), null);
        
        MerchantWebsiteConfig config = new MerchantWebsiteConfig();
        config.setMerchant(merchant);
        mapRequestToEntity(request, config);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        
        config = configRepository.save(config);
        initializeMerchantS3Folders(merchantId);
        
        log.info("Website configuration saved successfully for merchant: {}", merchantId);
        return mapEntityToResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public WebsiteConfigResponse getConfiguration(Integer merchantId) {
        log.info("Fetching website configuration for merchant: {}", merchantId);
        
        MerchantWebsiteConfig config = configRepository.findByMerchant_MerchantId(merchantId)
            .orElseThrow(() -> new ValidationException("Website configuration not found for merchant: " + merchantId));
        
        return mapEntityToResponse(config);
    }

    @Override
    @Transactional
    public WebsiteConfigResponse updateConfiguration(Integer merchantId, WebsiteConfigRequest request) {
        log.info("Updating website configuration for merchant: {}", merchantId);
        
        MerchantWebsiteConfig config = configRepository.findByMerchant_MerchantId(merchantId)
            .orElseThrow(() -> new ValidationException("Website configuration not found for merchant: " + merchantId));
        
        validateWebsiteAddress(request.getWebsiteAddress(), merchantId);
        
        mapRequestToEntity(request, config);
        config.setUpdatedAt(LocalDateTime.now());
        
        config = configRepository.save(config);
        
        log.info("Website configuration updated successfully for merchant: {}", merchantId);
        return mapEntityToResponse(config);
    }

    @Override
    @Transactional
    public WebsiteConfigResponse patchConfiguration(Integer merchantId, com.cloudkitchen.rbac.dto.website.WebsiteConfigUpdateRequest request) {
        log.info("Patching website configuration for merchant: {}", merchantId);
        
        MerchantWebsiteConfig config = configRepository.findByMerchant_MerchantId(merchantId)
            .orElseThrow(() -> new ValidationException("Website configuration not found for merchant: " + merchantId));
        
        if (request.getLogoUrl() != null) {
            config.setLogoUrl(request.getLogoUrl());
        }
        if (request.getBannerUrl() != null) {
            config.setBannerUrl(request.getBannerUrl());
        }
        if (request.getPhotoUrl() != null) {
            config.setPhotoUrl(request.getPhotoUrl());
        }
        
        config.setUpdatedAt(LocalDateTime.now());
        config = configRepository.save(config);
        
        log.info("Configuration patched successfully for merchant: {}", merchantId);
        return mapEntityToResponse(config);
    }

    @Override
    @Transactional
    public WebsiteConfigResponse publishWebsite(Integer merchantId) {
        log.info("Publishing website for merchant: {}", merchantId);
        
        MerchantWebsiteConfig config = configRepository.findByMerchant_MerchantId(merchantId)
            .orElseThrow(() -> new ValidationException("Website configuration not found for merchant: " + merchantId));
        
        if (config.getKitchenName() == null || config.getWebsiteAddress() == null) {
            throw new ValidationException("Cannot publish incomplete website configuration");
        }
        
        config.setIsPublished(true);
        config.setPublishedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        
        config = configRepository.save(config);
        
        log.info("Website published successfully for merchant: {}", merchantId);
        return mapEntityToResponse(config);
    }

    @Override
    public void initializeMerchantS3Folders(Integer merchantId) {
        log.info("Initializing S3 folder structure for merchant: {}", merchantId);
        
        try {
            cloudStorageService.createMerchantFolderStructure(merchantId.toString());
            log.info("S3 folder structure initialized for merchant: {}", merchantId);
        } catch (Exception e) {
            log.error("Failed to initialize S3 folders for merchant: {}", merchantId, e);
        }
    }

    private void validateWebsiteAddress(String websiteAddress, Integer merchantId) {
        if (merchantId == null) {
            if (configRepository.existsByWebsiteAddress(websiteAddress)) {
                throw new ValidationException("Website address already exists: " + websiteAddress);
            }
        } else {
            if (configRepository.existsByWebsiteAddressAndMerchant_MerchantIdNot(websiteAddress, merchantId)) {
                throw new ValidationException("Website address already exists: " + websiteAddress);
            }
        }
    }

    private void mapRequestToEntity(WebsiteConfigRequest request, MerchantWebsiteConfig config) {
        config.setKitchenName(request.getKitchenName());
        config.setLogoUrl(request.getLogoUrl());
        config.setBannerUrl(request.getBannerUrl());
        config.setPhotoUrl(request.getPhotoUrl());
        config.setWebsiteAddress(request.getWebsiteAddress());
        config.setDescription(request.getDescription());
        config.setAddress(request.getAddress());
        config.setWhatsapp(request.getWhatsapp());
        config.setInstagram(request.getInstagram());
        config.setYoutube(request.getYoutube());
        config.setTwitter(request.getTwitter());
        config.setFacebook(request.getFacebook());
    }

    private WebsiteConfigResponse mapEntityToResponse(MerchantWebsiteConfig config) {
        WebsiteConfigResponse response = new WebsiteConfigResponse();
        response.setId(config.getId());
        response.setMerchantId(config.getMerchant().getMerchantId());
        response.setKitchenName(config.getKitchenName());
        response.setLogoUrl(config.getLogoUrl());
        response.setBannerUrl(config.getBannerUrl());
        response.setPhotoUrl(config.getPhotoUrl());
        response.setWebsiteAddress(config.getWebsiteAddress());
        response.setDescription(config.getDescription());
        response.setAddress(config.getAddress());
        response.setWhatsapp(config.getWhatsapp());
        response.setInstagram(config.getInstagram());
        response.setYoutube(config.getYoutube());
        response.setTwitter(config.getTwitter());
        response.setFacebook(config.getFacebook());
        response.setIsPublished(config.getIsPublished());
        response.setPublishedAt(config.getPublishedAt());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());
        return response;
    }
}
