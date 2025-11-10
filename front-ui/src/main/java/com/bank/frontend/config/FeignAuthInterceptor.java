package com.bank.frontend.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignAuthInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpSession session = attributes.getRequest().getSession(false);
            if (session != null) {
                String accessToken = (String) session.getAttribute("access_token");
                if (accessToken != null) {
                    log.debug("FeignAuthInterceptor attaching bearer token for session {}", session.getId());
                    template.header("Authorization", "Bearer " + accessToken);
                }
                else {
                    log.debug("FeignAuthInterceptor: access token missing in session {}", session.getId());
                }
            } else {
                log.debug("FeignAuthInterceptor: no HTTP session available for request {}", attributes.getRequest().getRequestURI());
            }
        } else {
            log.debug("FeignAuthInterceptor: no request attributes available (non-web thread?)");
        }
    }
}
