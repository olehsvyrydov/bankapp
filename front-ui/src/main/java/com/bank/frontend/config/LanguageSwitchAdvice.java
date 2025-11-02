package com.bank.frontend.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;

@ControllerAdvice
public class LanguageSwitchAdvice {

  @ModelAttribute("languageLinks")
  public Map<String, String> languageLinks(HttpServletRequest request) {
    UriComponentsBuilder baseBuilder = UriComponentsBuilder.fromPath(request.getRequestURI());
    if ("GET".equalsIgnoreCase(request.getMethod())) {
      request
          .getParameterMap()
          .forEach(
              (key, values) -> {
                if ("lang".equals(key)) {
                  return;
                }
                for (String value : values) {
                  baseBuilder.queryParam(key, value);
                }
              });
    }

    Map<String, String> links = new HashMap<>();
    links.put(
        "en", baseBuilder.cloneBuilder().replaceQueryParam("lang", "en").build().toUriString());
    links.put(
        "ru", baseBuilder.cloneBuilder().replaceQueryParam("lang", "ru").build().toUriString());
    return links;
  }
}
