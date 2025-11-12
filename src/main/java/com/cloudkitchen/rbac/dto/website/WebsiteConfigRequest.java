package com.cloudkitchen.rbac.dto.website;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class WebsiteConfigRequest {

    @NotBlank(message = "Kitchen name is required")
    @Size(max = 255, message = "Kitchen name must not exceed 255 characters")
    private String kitchenName;

    @Size(max = 1000, message = "Logo URL must not exceed 1000 characters")
    private String logoUrl;

    @Size(max = 1000, message = "Banner URL must not exceed 1000 characters")
    private String bannerUrl;

    @Size(max = 1000, message = "Photo URL must not exceed 1000 characters")
    private String photoUrl;

    @NotBlank(message = "Website address is required")
    @Size(max = 500, message = "Website address must not exceed 500 characters")
    @Pattern(regexp = "^https?://.*", message = "Website address must be a valid URL")
    private String websiteAddress;

    private String description;

    private String address;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid WhatsApp number format")
    private String whatsapp;

    @Size(max = 255, message = "Instagram URL must not exceed 255 characters")
    private String instagram;

    @Size(max = 255, message = "YouTube URL must not exceed 255 characters")
    private String youtube;

    @Size(max = 255, message = "Twitter URL must not exceed 255 characters")
    private String twitter;

    @Size(max = 255, message = "Facebook URL must not exceed 255 characters")
    private String facebook;

    public String getKitchenName() { return kitchenName; }
    public void setKitchenName(String kitchenName) { this.kitchenName = kitchenName; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getWebsiteAddress() { return websiteAddress; }
    public void setWebsiteAddress(String websiteAddress) { this.websiteAddress = websiteAddress; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }

    public String getYoutube() { return youtube; }
    public void setYoutube(String youtube) { this.youtube = youtube; }

    public String getTwitter() { return twitter; }
    public void setTwitter(String twitter) { this.twitter = twitter; }

    public String getFacebook() { return facebook; }
    public void setFacebook(String facebook) { this.facebook = facebook; }
}
