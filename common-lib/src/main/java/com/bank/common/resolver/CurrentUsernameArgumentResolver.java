package com.bank.common.resolver;

import com.bank.common.annotations.CurrentUsername;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class CurrentUsernameArgumentResolver implements HandlerMethodArgumentResolver
{
    @Override
    public boolean supportsParameter(MethodParameter parameter)
    {
        return parameter.hasParameterAnnotation(CurrentUsername.class)
            && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory) throws Exception
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AnonymousAuthenticationToken token)
        {
            log.warn("resolveUsername: authentication missing or anonymous. Authentication={}", token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        log.debug("resolveUsername: authenticated user={}, type={}",
            auth.getName(), auth.getClass().getSimpleName());
        return auth.getName();
    }
}
