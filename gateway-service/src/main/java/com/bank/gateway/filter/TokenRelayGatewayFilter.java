package com.bank.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that forwards JWT tokens from incoming requests to downstream services.
 * This filter extracts the JWT from the security context and adds it as Authorization header.
 */
@Component
public class TokenRelayGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TokenRelayGatewayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(JwtAuthenticationToken.class::isInstance)
            .map(JwtAuthenticationToken.class::cast)
            .map(JwtAuthenticationToken::getToken)
            .map(Jwt::getTokenValue)
            .map(token -> {
                log.debug("Forwarding JWT token to downstream service for path: {}",
                    exchange.getRequest().getPath());

                ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();

                return exchange.mutate().request(request).build();
            })
            .defaultIfEmpty(exchange)
            .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        // Run after authentication filter
        return -1;
    }
}

