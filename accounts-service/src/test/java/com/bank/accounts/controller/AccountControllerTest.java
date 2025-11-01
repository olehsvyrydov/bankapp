package com.bank.accounts.controller;

import com.bank.common.dto.contracts.accounts.AccountDTO;
import com.bank.common.dto.contracts.accounts.CreateAccountRequest;
import com.bank.accounts.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@WithMockUser
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @Test
    void testRegister_Success() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .email("test@example.com")
            .birthDate(LocalDate.of(2000, 1, 1))
            .build();

        AccountDTO accountDTO = AccountDTO.builder()
            .id(1L)
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .build();

        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(accountDTO);

        mockMvc.perform(post("/api/accounts/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void testGetCurrentAccount_Success() throws Exception {
        AccountDTO accountDTO = AccountDTO.builder()
            .id(1L)
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .build();

        when(accountService.getAccountByUsername(anyString())).thenReturn(accountDTO);

        mockMvc.perform(get("/api/accounts/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("testuser"));
    }
}
