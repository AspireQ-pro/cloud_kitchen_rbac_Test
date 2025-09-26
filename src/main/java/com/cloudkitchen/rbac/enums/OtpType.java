package com.cloudkitchen.rbac.enums;

public enum OtpType {
    LOGIN("login"),
    REGISTRATION("registration"),
    PASSWORD_RESET("password_reset"),
    PHONE_VERIFICATION("phone_verification");
    
    private final String value;
    
    OtpType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static OtpType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("OTP type value cannot be null");
        }
        for (OtpType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid OTP type: " + value);
    }
}