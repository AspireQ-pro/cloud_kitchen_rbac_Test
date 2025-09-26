package com.cloudkitchen.rbac.enums;

public enum UserType {
    SUPER_ADMIN("super_admin"),
    MERCHANT("merchant"),
    CUSTOMER("customer");
    
    private final String value;
    
    UserType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static UserType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("User type value cannot be null");
        }
        for (UserType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid user type: " + value);
    }
}