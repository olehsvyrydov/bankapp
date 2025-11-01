package com.bank.frontend.config;

import com.bank.frontend.interceptor.AuthenticationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for registering interceptors.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
            .addPathPatterns(
                "/home/**",
                "/account/**",
                "/bank-account/**",
                "/transfer/**",
                "/cash/**",
                "/api/**"
            )
            .excludePathPatterns(
                "/",
                "/login",
                "/register",
                "/perform-login",
                "/perform-register",
                "/logout",
                "/error",
                "/favicon.ico",
                "/css/**",
                "/js/**",
                "/images/**",
                "/webjars/**"
            );
    }
}

