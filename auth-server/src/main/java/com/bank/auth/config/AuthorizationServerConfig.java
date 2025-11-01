package com.bank.auth.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.util.Base64URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoders;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Configuration
public class AuthorizationServerConfig {

//    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);
//    private static final String AUTH_SCHEMA = "auth";

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

//        ensureAuthorizationServerTables(jdbcTemplate);

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

//        ensureAuthorizationServerTables(jdbcTemplate);
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
        JdbcTemplate jdbcTemplate,
        RegisteredClientRepository registeredClientRepository) {

//        ensureAuthorizationServerTables(jdbcTemplate);
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
            .scopes(existing -> Stream.of(scopes).forEach(existing::add))
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
            .scopes(existing -> Stream.of(scopes).forEach(existing::add))
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
            desiredClient.getClientAuthenticationMethods().forEach(methods::add);
        });

        builder.authorizationGrantTypes(grantTypes -> {
            grantTypes.clear();
            desiredClient.getAuthorizationGrantTypes().forEach(grantTypes::add);
        });

        builder.scopes(scopes -> {
            scopes.clear();
            desiredClient.getScopes().forEach(scopes::add);
        });

        builder.redirectUris(uris -> {
            uris.clear();
            desiredClient.getRedirectUris().forEach(uris::add);
        });

        builder.postLogoutRedirectUris(uris -> {
            uris.clear();
            desiredClient.getPostLogoutRedirectUris().forEach(uris::add);
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

//    private void ensureAuthorizationServerTables(JdbcTemplate jdbcTemplate) {
//        try {
//            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + AUTH_SCHEMA);
//            jdbcTemplate.execute("SET search_path TO " + AUTH_SCHEMA);
//
//            jdbcTemplate.execute("""
//                CREATE TABLE IF NOT EXISTS auth.oauth2_registered_client (
//                    id VARCHAR(100) PRIMARY KEY,
//                    client_id VARCHAR(100) NOT NULL,
//                    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
//                    client_secret VARCHAR(200) DEFAULT NULL,
//                    client_secret_expires_at TIMESTAMP DEFAULT NULL,
//                    client_name VARCHAR(200) NOT NULL,
//                    client_authentication_methods VARCHAR(1000) NOT NULL,
//                    authorization_grant_types VARCHAR(1000) NOT NULL,
//                    redirect_uris VARCHAR(1000) DEFAULT NULL,
//                    post_logout_redirect_uris VARCHAR(1000) DEFAULT NULL,
//                    scopes VARCHAR(1000) NOT NULL,
//                    client_settings VARCHAR(2000) NOT NULL,
//                    token_settings VARCHAR(2000) NOT NULL
//                )
//            """);
//
//            jdbcTemplate.execute("""
//                CREATE UNIQUE INDEX IF NOT EXISTS oauth2_registered_client_client_id_idx
//                    ON auth.oauth2_registered_client (client_id)
//            """);
//
//            jdbcTemplate.execute("""
//                CREATE TABLE IF NOT EXISTS auth.oauth2_authorization (
//                    id VARCHAR(100) PRIMARY KEY,
//                    registered_client_id VARCHAR(100) NOT NULL,
//                    principal_name VARCHAR(200) NOT NULL,
//                    authorization_grant_type VARCHAR(100) NOT NULL,
//                    authorized_scopes VARCHAR(1000) DEFAULT NULL,
//                    attributes TEXT DEFAULT NULL,
//                    state VARCHAR(500) DEFAULT NULL,
//                    authorization_code_value BYTEA DEFAULT NULL,
//                    authorization_code_issued_at TIMESTAMP DEFAULT NULL,
//                    authorization_code_expires_at TIMESTAMP DEFAULT NULL,
//                    authorization_code_metadata TEXT DEFAULT NULL,
//                    access_token_value BYTEA DEFAULT NULL,
//                    access_token_issued_at TIMESTAMP DEFAULT NULL,
//                    access_token_expires_at TIMESTAMP DEFAULT NULL,
//                    access_token_metadata TEXT DEFAULT NULL,
//                    access_token_type VARCHAR(100) DEFAULT NULL,
//                    access_token_scopes VARCHAR(1000) DEFAULT NULL,
//                    refresh_token_value BYTEA DEFAULT NULL,
//                    refresh_token_issued_at TIMESTAMP DEFAULT NULL,
//                    refresh_token_expires_at TIMESTAMP DEFAULT NULL,
//                    refresh_token_metadata TEXT DEFAULT NULL,
//                    oidc_id_token_value BYTEA DEFAULT NULL,
//                    oidc_id_token_issued_at TIMESTAMP DEFAULT NULL,
//                    oidc_id_token_expires_at TIMESTAMP DEFAULT NULL,
//                    oidc_id_token_metadata TEXT DEFAULT NULL,
//                    oidc_id_token_claims TEXT DEFAULT NULL,
//                    user_code_value BYTEA DEFAULT NULL,
//                    user_code_issued_at TIMESTAMP DEFAULT NULL,
//                    user_code_expires_at TIMESTAMP DEFAULT NULL,
//                    user_code_metadata TEXT DEFAULT NULL,
//                    device_code_value BYTEA DEFAULT NULL,
//                    device_code_issued_at TIMESTAMP DEFAULT NULL,
//                    device_code_expires_at TIMESTAMP DEFAULT NULL,
//                    device_code_metadata TEXT DEFAULT NULL
//                )
//            """);
//
//            jdbcTemplate.execute("""
//                CREATE TABLE IF NOT EXISTS auth.oauth2_authorization_consent (
//                    registered_client_id VARCHAR(100) NOT NULL,
//                    principal_name VARCHAR(200) NOT NULL,
//                    authorities VARCHAR(1000) NOT NULL,
//                    PRIMARY KEY (registered_client_id, principal_name)
//                )
//            """);
//
//        } catch (Exception ex) {
//            log.warn("Failed to ensure OAuth2 tables exist before initialization: {}", ex.getMessage());
//        }
//    }
}
