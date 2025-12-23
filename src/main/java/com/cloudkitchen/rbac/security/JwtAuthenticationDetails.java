package com.cloudkitchen.rbac.security;

import org.springframework.security.web.authentication.WebAuthenticationDetails;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Custom authentication details to store JWT claims like merchantId
 */
public class JwtAuthenticationDetails extends WebAuthenticationDetails {

    private static final long serialVersionUID = 1L;

    private final Integer merchantId;

    public JwtAuthenticationDetails(HttpServletRequest request, Integer merchantId) {
        super(request);
        this.merchantId = merchantId;
    }

    public Integer getMerchantId() {
        return merchantId;
    }
}
