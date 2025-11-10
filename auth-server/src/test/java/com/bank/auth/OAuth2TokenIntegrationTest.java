package com.bank.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2TokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetTokenWithClientCredentials() throws Exception {
        // Basic Auth header
        String credentials = "gateway-service:gateway-secret";
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        MvcResult result = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", "Basic " + encodedCredentials)
                .param("grant_type", "client_credentials")
                .param("scope", "gateway")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        // Verify response contains required fields
        assertThat(jsonNode.has("access_token")).isTrue();
        assertThat(jsonNode.has("token_type")).isTrue();
        assertThat(jsonNode.has("expires_in")).isTrue();
        assertThat(jsonNode.get("token_type").asText()).isEqualTo("Bearer");
        assertThat(jsonNode.get("scope").asText()).contains("gateway");

        // Token should not be empty
        String accessToken = jsonNode.get("access_token").asText();
        assertThat(accessToken).isNotEmpty();

        System.out.println("Successfully obtained access token: " + accessToken.substring(0, 20) + "...");
    }
}
