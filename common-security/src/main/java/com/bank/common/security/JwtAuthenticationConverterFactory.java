package com.bank.common.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Factory for creating preconfigured {@link JwtAuthenticationConverter} instances that align with
 * the gateway-wide scope naming conventions.
 */
public final class JwtAuthenticationConverterFactory {

  private static final String DEFAULT_AUTHORITY_PREFIX = "SCOPE_";
  private static final String DEFAULT_AUTHORITIES_CLAIM = "scope";

  private JwtAuthenticationConverterFactory() {}

  public static JwtAuthenticationConverter scopeBased() {
    return scopeBased(DEFAULT_AUTHORITY_PREFIX, DEFAULT_AUTHORITIES_CLAIM);
  }

  public static JwtAuthenticationConverter scopeBased(
      String authorityPrefix, String authoritiesClaimName) {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthorityPrefix(authorityPrefix);
    authoritiesConverter.setAuthoritiesClaimName(authoritiesClaimName);

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }
}
