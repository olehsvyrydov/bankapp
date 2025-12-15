package com.bank.gateway.config;

import static com.bank.common.security.JwtAuthenticationConverterFactory.scopeBased;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            registry ->
                registry
                    .pathMatchers("/actuator/**")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/exchange/rates")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/accounts/register")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/exchange/rates")
                    .hasAnyAuthority("SCOPE_gateway", "SCOPE_exchange.generate")
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(
            oauth ->
                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .build();
  }

  private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
    return new ReactiveJwtAuthenticationConverterAdapter(scopeBased());
  }
}
