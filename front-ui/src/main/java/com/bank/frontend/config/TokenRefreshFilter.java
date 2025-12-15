package com.bank.frontend.config;

import com.bank.common.auth.TokenResponse;
import com.bank.frontend.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refreshes expiring access tokens using the refresh token stored in session.
 * Runs before security authentication so the security context always sees fresh tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenRefreshFilter extends OncePerRequestFilter {

  private final AuthService authService;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    HttpSession session = request.getSession(false);
    if (session != null) {
      String accessToken = (String) session.getAttribute("access_token");
      String refreshToken = (String) session.getAttribute("refresh_token");

      if (shouldAttemptRefresh(accessToken, refreshToken)) {
        log.debug("Attempting to refresh access token for {}", request.getRequestURI());
        try {
          var result = authService.refreshToken(refreshToken);
          if (!result.isSuccess()) {
            log.warn("Refresh token rejected; redirecting to login");
            invalidateSession(session);
            response.sendRedirect("/login?sessionExpired=true");
            return;
          }

          TokenResponse tokens = result.getData();
          if (tokens == null || !StringUtils.hasText(tokens.accessToken())) {
            log.warn("Refresh response missing access token; redirecting to login");
            invalidateSession(session);
            response.sendRedirect("/login?sessionExpired=true");
            return;
          }

          session.setAttribute("access_token", tokens.accessToken());
          session.setAttribute("refresh_token", tokens.refreshToken());
          if (StringUtils.hasText(tokens.scope())) {
            session.setAttribute("token_scope", tokens.scope());
          }
          log.debug("Access token refreshed successfully");
        } catch (Exception ex) {
          log.error("Token refresh failed", ex);
          invalidateSession(session);
          response.sendRedirect("/login?sessionExpired=true");
          return;
        }
      }
    }

    filterChain.doFilter(request, response);
  }

  private boolean shouldAttemptRefresh(String accessToken, String refreshToken) {
    if (!StringUtils.hasText(accessToken) || !StringUtils.hasText(refreshToken)) {
      return false;
    }
    return isExpiringSoon(accessToken);
  }

  private boolean isExpiringSoon(String jwt) {
    try {
      String[] parts = jwt.split("\\.");
      if (parts.length < 2) {
        return true;
      }

      byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
      JsonNode payload = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
      JsonNode exp = payload.get("exp");
      if (exp == null || !exp.isNumber()) {
        return true;
      }

      long secondsRemaining = exp.asLong() - (System.currentTimeMillis() / 1000);
      return secondsRemaining < 60;
    } catch (Exception ex) {
      log.debug("Failed to parse JWT payload", ex);
      return true;
    }
  }

  private void invalidateSession(HttpSession session) {
    try {
      session.invalidate();
    } catch (IllegalStateException ignored) {
      // Session already invalidated.
    }
  }
}
