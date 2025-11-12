package com.cloudkitchen.rbac.dto.website;

import jakarta.validation.constraints.Size;

/**
 * DTO for partial updates (used by Order Service to update asset URLs)
 */
public class WebsiteConfigUpdateRequest {

    @Size(max = 1000, message = "Logo URL must not exceed 1000 characters")
    private String logoUrl;

    @Size(max = 1000, message = "Banner URL must not exceed 1000 characters")
    private String bannerUrl;

    @Size(max = 1000, message = "Photo URL must not exceed 1000 characters")
    private String photoUrl;

    // Getters and Setters
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
