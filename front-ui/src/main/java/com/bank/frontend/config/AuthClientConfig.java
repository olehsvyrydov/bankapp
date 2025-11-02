package com.bank.frontend.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
public class AuthClientConfig {
    @Bean
    public RequestInterceptor basicAuthRequestInterceptor() {
        return template -> {
            // Only add Basic Auth if it's the token endpoint
            if (template.url().contains("/oauth2/token")) {
                template.header("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString(
                        "frontend-client:secret".getBytes()));
            }
        };
    }
}
