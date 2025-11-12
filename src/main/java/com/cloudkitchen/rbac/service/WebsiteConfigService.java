package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.dto.website.WebsiteConfigRequest;
import com.cloudkitchen.rbac.dto.website.WebsiteConfigResponse;
import com.cloudkitchen.rbac.dto.website.WebsiteConfigUpdateRequest;

public interface WebsiteConfigService {
    
    WebsiteConfigResponse saveConfiguration(Integer merchantId, WebsiteConfigRequest request);
    
    WebsiteConfigResponse getConfiguration(Integer merchantId);
    
    WebsiteConfigResponse updateConfiguration(Integer merchantId, WebsiteConfigRequest request);
    
    WebsiteConfigResponse patchConfiguration(Integer merchantId, WebsiteConfigUpdateRequest request);
    
    WebsiteConfigResponse publishWebsite(Integer merchantId);
    
    void initializeMerchantS3Folders(Integer merchantId);
}
