package com.bank.common.config;

import feign.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
@ConditionalOnProperty(name = "security.internal-client.registration-id")
public class OAuth2FeignConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuth2FeignConfig.class);

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    private final String registrationId;
    private final boolean forwardUserToken;

    public OAuth2FeignConfig(ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientService authorizedClientService,
        @Value("${security.internal-client.registration-id:#{null}}") String registrationId,
        @Value("${security.internal-client.forward-user-token:true}") boolean forwardUserToken) {

        // If no registration ID is configured, use the application name
        if (registrationId == null || registrationId.isEmpty()) {
            this.registrationId = null;
            this.forwardUserToken = forwardUserToken;
            this.authorizedClientManager = null;
            log.warn("OAuth2FeignConfig: No registration-id configured, OAuth2 client credentials flow will be disabled");
            return;
        }

        this.registrationId = registrationId;
        this.forwardUserToken = forwardUserToken;
        log.info("Initializing OAuth2FeignConfig for registration {}", registrationId);

        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(authorizedClientProvider);

        this.authorizedClientManager = manager;
    }

    @Bean
    public RequestInterceptor oauth2FeignRequestInterceptor() {
        return requestTemplate -> {
            // If OAuth2 is not configured, skip
            if (authorizedClientManager == null || registrationId == null) {
                log.debug("OAuth2FeignConfig: OAuth2 client not configured, skipping token acquisition");
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("OAuth2FeignConfig intercepting request to {}", requestTemplate.url());

            JwtAuthenticationToken jwtAuth = authentication instanceof JwtAuthenticationToken
                ? (JwtAuthenticationToken) authentication
                : null;

            if (jwtAuth != null) {
                if (forwardUserToken) {
                    log.debug("OAuth2FeignConfig forwarding user JWT token");
                    requestTemplate.header("Authorization", "Bearer " + jwtAuth.getToken().getTokenValue());
                    return;
                }
                requestTemplate.header("X-User-Name", jwtAuth.getName());
            }

            try {
                OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(registrationId)
                    .principal(registrationId)
                    .build();

                OAuth2AuthorizedClient authorizedClient =
                    authorizedClientManager.authorize(authorizeRequest);

                if (authorizedClient != null) {
                    OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                    log.debug("OAuth2FeignConfig obtained client token for {} expiring at {}", registrationId, accessToken.getExpiresAt());
                    requestTemplate.header("Authorization", "Bearer " + accessToken.getTokenValue());
                } else {
                    log.warn("Failed to obtain client credentials token for registration {}", registrationId);
                }
            } catch (Exception e) {
                log.error("Error obtaining OAuth2 token for registration {}: {}", registrationId, e.getMessage(), e);
            }
        };
    }
}
