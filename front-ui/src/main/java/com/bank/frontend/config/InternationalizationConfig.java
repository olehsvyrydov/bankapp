package com.bank.frontend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Locale;

/**
 * Configuration for internationalization (i18n).
 * Supports multiple languages with cookie-based locale storage.
 */
@Configuration
public class InternationalizationConfig implements WebMvcConfigurer {

    /**
     * Configures locale resolver to store user's language preference in a cookie.
     * Default locale is English (en).
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("user-lang");
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }

    /**
     * Interceptor to change locale based on 'lang' parameter in URL.
     * Example: ?lang=ru or ?lang=en
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}

