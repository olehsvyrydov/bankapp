package com.bank.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServerConfigTest {

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private AuthorizationServerSettings authorizationServerSettings;

    @Test
    void contextLoads() {
        assertThat(registeredClientRepository).isNotNull();
    }

    @Test
    void shouldHaveGatewayClientRegistered() {
        var client = registeredClientRepository.findByClientId("gateway-service");
        assertThat(client).isNotNull();
        assertThat(client.getClientId()).isEqualTo("gateway-service");
    }

    @Test
    void shouldHaveFrontUiClientConfigured() {
        var client = registeredClientRepository.findByClientId("front-ui");
        assertThat(client).isNotNull();
        assertThat(client.getRedirectUris()).contains("http://localhost:8090/login/oauth2/code/front-ui");
        assertThat(client.getAuthorizationGrantTypes())
            .anyMatch(grantType -> AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(grantType.getValue()));
    }

    @Test
    void shouldHaveAuthorizationServerSettings() {
        assertThat(authorizationServerSettings).isNotNull();
        assertThat(authorizationServerSettings.getIssuer()).isEqualTo("http://bank-app-auth-server:9100");
    }
}
