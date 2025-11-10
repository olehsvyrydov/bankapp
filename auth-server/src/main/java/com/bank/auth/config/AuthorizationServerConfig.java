package com.bank.auth.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.util.Base64URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    private final String issuer;

    public AuthorizationServerConfig(@Value("${security.issuer}") String issuer) {
        this.issuer = issuer;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());

        http
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(org.springframework.http.MediaType.TEXT_HTML)
                )
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer(issuer)
            .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
        JdbcTemplate jdbcTemplate,
        PasswordEncoder passwordEncoder) {

        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        List<RegisteredClient> defaultClients = List.of(
            clientCredentialsClient(
                "gateway-service",
                "Gateway Service",
                "gateway-secret",
                Duration.ofMinutes(30),
                Duration.ofHours(12),
                passwordEncoder,
                "gateway"
            ),
            clientCredentialsClient(
                "accounts-service",
                "Accounts Service",
                "accounts-secret",
                Duration.ofMinutes(15),
                null,
                passwordEncoder,
                "accounts.read",
                "accounts.write",
                "notifications.send"
            ),
            clientCredentialsClient(
                "cash-service",
                "Cash Service",
                "cash-secret",
                Duration.ofMinutes(15),
                null,
                passwordEncoder,
                "cash.write",
                "cash.read",
                "accounts.read",
                "accounts.write",
                "notifications.send"
            ),
            clientCredentialsClient(
                "transfer-service",
                "Transfer Service",
                "transfer-secret",
                Duration.ofMinutes(15),
                null,
                passwordEncoder,
                "transfer.write",
                "transfer.read",
                "accounts.read",
                "accounts.write",
                "notifications.send",
                "exchange.read"
            ),
            clientCredentialsClient(
                "exchange-service",
                "Exchange Service",
                "exchange-secret",
                Duration.ofMinutes(15),
                null,
                passwordEncoder,
                "exchange.read"
            ),
            clientCredentialsClient(
                "exchange-generator-service",
                "Exchange Generator Service",
                "exchange-generator-secret",
                Duration.ofMinutes(15),
                null,
                passwordEncoder,
                "exchange.generate",
                "exchange.read",
                "gateway"
            ),
            clientCredentialsClient(
                "notifications-service",
                "Notifications Service",
                "notifications-secret",
                Duration.ofMinutes(15),
                null,
                passwordEncoder,
                "notifications.send"
            ),
            clientCredentialsClient(
                "blocker-service",
                "Blocker Service",
                "blocker-secret",
                Duration.ofMinutes(15),
                null,
                passwordEncoder,
                "blocker.check"
            ),
            authorizationCodeClient(
                "front-ui",
                "Front UI",
                "front-secret",
                passwordEncoder,
                List.of(
                    "http://localhost:8090/login/oauth2/code/front-ui",
                    "http://front-ui:8090/login/oauth2/code/front-ui"
                ),
                List.of(
                    "http://localhost:8090",
                    "http://front-ui:8090"
                ),
                "user.read",
                "user.write"
            )
        );

        defaultClients.forEach(client -> upsertRegisteredClient(repository, client));

        return repository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
        JdbcTemplate jdbcTemplate,
        RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
        JdbcTemplate jdbcTemplate,
        RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public JWKSource<com.nimbusds.jose.proc.SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        JWK jwk = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(Base64URL.encode(publicKey.getModulus()).toString())
            .build();

        return new ImmutableJWKSet<>(new JWKSet(jwk));
    }

    private RegisteredClient clientCredentialsClient(
        String clientId,
        String clientName,
        String rawSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        PasswordEncoder passwordEncoder,
        String... scopes) {

        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder()
            .accessTokenTimeToLive(accessTokenTtl);

        if (refreshTokenTtl != null) {
            tokenSettingsBuilder.refreshTokenTimeToLive(refreshTokenTtl);
        }

        ClientSettings clientSettings = ClientSettings.builder()
            .requireAuthorizationConsent(false)
            .build();

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(clientId)
            .clientName(clientName)
            .clientSecret(passwordEncoder.encode(rawSecret))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scopes(existing -> existing.addAll(Arrays.asList(scopes)))
            .tokenSettings(tokenSettingsBuilder.build())
            .clientSettings(clientSettings);

        if (refreshTokenTtl != null) {
            builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
        }

        return builder.build();
    }

    private RegisteredClient authorizationCodeClient(
        String clientId,
        String clientName,
        String rawSecret,
        PasswordEncoder passwordEncoder,
        List<String> redirectUris,
        List<String> postLogoutRedirectUris,
        String... scopes) {

        TokenSettings tokenSettings = TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofMinutes(15))
            .refreshTokenTimeToLive(Duration.ofHours(12))
            .reuseRefreshTokens(false)
            .build();

        ClientSettings clientSettings = ClientSettings.builder()
            .requireAuthorizationConsent(true)
            .requireProofKey(false)
            .build();

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(clientId)
            .clientName(clientName)
            .clientSecret(passwordEncoder.encode(rawSecret))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .scopes(existing -> existing.addAll(Arrays.asList(scopes)))
            .tokenSettings(tokenSettings)
            .clientSettings(clientSettings);

        redirectUris.forEach(builder::redirectUri);
        postLogoutRedirectUris.forEach(builder::postLogoutRedirectUri);

        return builder.build();
    }

    private void upsertRegisteredClient(
        JdbcRegisteredClientRepository repository,
        RegisteredClient desiredClient) {

        RegisteredClient existing = repository.findByClientId(desiredClient.getClientId());
        if (existing == null) {
            repository.save(desiredClient);
            return;
        }

        RegisteredClient.Builder builder = RegisteredClient.from(existing)
            .clientSecret(desiredClient.getClientSecret())
            .clientName(desiredClient.getClientName())
            .tokenSettings(desiredClient.getTokenSettings())
            .clientSettings(desiredClient.getClientSettings());

        builder.clientAuthenticationMethods(methods -> {
            methods.clear();
            methods.addAll(desiredClient.getClientAuthenticationMethods());
        });

        builder.authorizationGrantTypes(grantTypes -> {
            grantTypes.clear();
            grantTypes.addAll(desiredClient.getAuthorizationGrantTypes());
        });

        builder.scopes(scopes -> {
            scopes.clear();
            scopes.addAll(desiredClient.getScopes());
        });

        builder.redirectUris(uris -> {
            uris.clear();
            uris.addAll(desiredClient.getRedirectUris());
        });

        builder.postLogoutRedirectUris(uris -> {
            uris.clear();
            uris.addAll(desiredClient.getPostLogoutRedirectUris());
        });

        repository.save(builder.build());
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }
}
