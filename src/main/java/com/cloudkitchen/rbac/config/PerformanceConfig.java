package com.cloudkitchen.rbac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class PerformanceConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(false);
        filter.setIncludePayload(false);
        filter.setIncludeHeaders(false);
        filter.setIncludeClientInfo(true);
        filter.setMaxPayloadLength(0);
        return filter;
    }
}