package com.bank.frontend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Populates relative links for language switching without relying on removed Thymeleaf request objects.
 */
@ControllerAdvice
public class LanguageSwitchAdvice {

    public static final String LANGUAGE_BASE_PATH_ATTRIBUTE = "languageBasePath";

    @ModelAttribute("languageLinks")
    public Map<String, String> languageLinks(HttpServletRequest request) {
        String basePath = (String) request.getAttribute(LANGUAGE_BASE_PATH_ATTRIBUTE);
        if (basePath == null || basePath.isBlank()) {
            basePath = request.getRequestURI();
        }

        UriComponentsBuilder baseBuilder = UriComponentsBuilder.fromPath(basePath);
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            request.getParameterMap().forEach((key, values) -> {
                if ("lang".equals(key)) {
                    return;
                }
                for (String value : values) {
                    baseBuilder.queryParam(key, value);
                }
            });
        }

        Map<String, String> links = new HashMap<>();
        links.put("en", baseBuilder.cloneBuilder().replaceQueryParam("lang", "en").build().toUriString());
        links.put("ru", baseBuilder.cloneBuilder().replaceQueryParam("lang", "ru").build().toUriString());
        return links;
    }
}
