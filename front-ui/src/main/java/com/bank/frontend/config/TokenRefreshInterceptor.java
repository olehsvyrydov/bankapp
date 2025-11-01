
package com.bank.frontend.config;

import com.bank.common.dto.contracts.auth.TokenResponse;
import com.bank.frontend.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Base64;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenRefreshInterceptor implements HandlerInterceptor
{

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
        HttpServletResponse response,
        Object handler) throws Exception
    {
        HttpSession session = request.getSession(false);

        if (session != null)
        {
            String accessToken = (String) session.getAttribute("access_token");
            String refreshToken = (String) session.getAttribute("refresh_token");

            if (accessToken != null && isTokenExpired(accessToken))
            {
                try
                {
                    var refreshedTokenResponse = authService.refreshToken(refreshToken);
                    if (!refreshedTokenResponse.isSuccess())
                    {
                        log.warn("Refresh token is invalid; redirecting to login");
                        response.sendRedirect("/login");
                        return false;
                    }
                    TokenResponse newTokens = refreshedTokenResponse.getData();
                    if (newTokens != null && newTokens.accessToken() != null)
                    {
                        session.setAttribute("access_token", newTokens.accessToken());
                        session.setAttribute("refresh_token", newTokens.refreshToken());
                        log.info("Token refreshed for user session");
                    }
                    else
                    {
                        log.warn("Refresh token request returned no tokens; redirecting to login");
                        response.sendRedirect("/login");
                        return false;
                    }
                }
                catch (Exception e)
                {
                    log.error("Failed to refresh token", e);
                    response.sendRedirect("/login");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            JsonNode json = objectMapper.readTree(payload);
            JsonNode exp = json.get("exp");
            if (exp != null) {
                long expTime = exp.asLong();
                return (expTime - System.currentTimeMillis() / 1000) < 60;
            }

            return true;

        } catch (Exception e) {
            return true;
        }
    }


}
