package com.bank.frontend.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to check if user is authenticated before accessing protected resources.
 * Redirects to login page if user is not authenticated.
 */
@Slf4j
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        String requestURI = request.getRequestURI();
        log.debug("AuthenticationInterceptor checking access to: {}", requestURI);

        // Check if user is authenticated
        if (session == null || session.getAttribute("access_token") == null) {
            log.debug("User not authenticated, redirecting to login");
            response.sendRedirect(request.getContextPath() + "/login?sessionExpired=true");
            return false;
        }

        log.debug("User authenticated, allowing access");
        return true;
    }
}

