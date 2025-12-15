package com.bank.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
        // FIX: Use JwtAuthenticationConverter to properly extract authentication
        this.jwtAuthenticationConverter = JwtAuthenticationConverterFactory.scopeBased();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Jwt jwt = jwtDecoder.decode(token);
                // FIX: Use converter to properly extract authorities and principal
                JwtAuthenticationToken authentication =
                    (JwtAuthenticationToken) jwtAuthenticationConverter.convert(jwt);

                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    if (logger.isDebugEnabled()) {
                        logger.debug("JWT authentication successful for user: " + authentication.getName());
                    }
                }
            } catch (Exception e) {
                // Token validation failed
                logger.error("JWT validation failed", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
