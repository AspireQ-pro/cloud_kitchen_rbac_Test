package com.cloudkitchen.rbac.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_users_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_users_phone", columnList = "phone"),
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_active", columnList = "is_active"),
        @Index(name = "idx_users_user_type", columnList = "user_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_merchant_phone", columnNames = {"merchant_id", "phone"})
    }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "address", length = 250)
    private String address;
    
    @Column(name = "favorite_food", length = 100)
    private String favoriteFood;
    
    @Column(name = "dietary_preferences", length = 200)
    private String dietaryPreferences;
    
    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    @Check(constraints = "gender IN ('male', 'female', 'other')")
    private String gender;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "user_type", length = 20)
    @Check(constraints = "user_type IN ('super_admin', 'merchant', 'customer')")
    private String userType = "customer";

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "is_verified")
    private Boolean verified = false;

    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;
    
    @Column(name = "otp_code", length = 4)
    private String otpCode;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_attempts")
    private Integer otpAttempts = 0;

    @Column(name = "otp_blocked_until")
    private LocalDateTime otpBlockedUntil;
    
    @Column(name = "otp_used")
    private Boolean otpUsed = false;

    // Guest user
    @Column(name = "is_guest")
    private Boolean guest = false;

    @Column(name = "guest_converted_at")
    private LocalDateTime guestConvertedAt;

    // Login preference
    @Column(name = "preferred_login_method", length = 20)
    @Check(constraints = "preferred_login_method IN ('password', 'otp', 'both')")
    private String preferredLoginMethod = "otp";

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "created_on", updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdOn;

    @Column(name = "updated_on", insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedOn;

    // Getters & Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Merchant getMerchant() { return merchant; }
    public void setMerchant(Merchant merchant) { this.merchant = merchant; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getFavoriteFood() { return favoriteFood; }
    public void setFavoriteFood(String favoriteFood) { this.favoriteFood = favoriteFood; }
    
    public String getDietaryPreferences() { return dietaryPreferences; }
    public void setDietaryPreferences(String dietaryPreferences) { this.dietaryPreferences = dietaryPreferences; }
    
    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public Boolean getPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(Boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    // Security-sensitive getters removed to prevent accidental exposure
    // Use dedicated service methods for token validation

    public LocalDateTime getPasswordResetExpiresAt() { return passwordResetExpiresAt; }
    public void setPasswordResetExpiresAt(LocalDateTime passwordResetExpiresAt) { this.passwordResetExpiresAt = passwordResetExpiresAt; }

    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }

    public Integer getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(Integer otpAttempts) { this.otpAttempts = otpAttempts; }

    public LocalDateTime getOtpBlockedUntil() { return otpBlockedUntil; }
    public void setOtpBlockedUntil(LocalDateTime otpBlockedUntil) { this.otpBlockedUntil = otpBlockedUntil; }
    
    public Boolean getOtpUsed() { return otpUsed; }
    public void setOtpUsed(Boolean otpUsed) { this.otpUsed = otpUsed; }

    public Boolean getGuest() { return guest; }
    public void setGuest(Boolean guest) { this.guest = guest; }

    public LocalDateTime getGuestConvertedAt() { return guestConvertedAt; }
    public void setGuestConvertedAt(LocalDateTime guestConvertedAt) { this.guestConvertedAt = guestConvertedAt; }

    public String getPreferredLoginMethod() { return preferredLoginMethod; }
    public void setPreferredLoginMethod(String preferredLoginMethod) { this.preferredLoginMethod = preferredLoginMethod; }

    // Convenience methods for boolean fields
    public Boolean isActive() { return active; }
    public Boolean isVerified() { return verified; }
    public Boolean isPhoneVerified() { return phoneVerified; }
    public Boolean isEmailVerified() { return emailVerified; }
    public Boolean isGuest() { return guest; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public Integer getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }

    public LocalDateTime getUpdatedOn() { return updatedOn; }
    public void setUpdatedOn(LocalDateTime updatedOn) { this.updatedOn = updatedOn; }
}
