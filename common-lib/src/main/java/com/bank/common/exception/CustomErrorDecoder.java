package com.bank.common.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class CustomErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 401) {
            return new BadCredentialsException("Authentication required");
        }

        if (response.status() == 403) {
            return new AccessDeniedException("Access denied");
        }

        if (response.status() >= 400 && response.status() < 500) {
            return new BusinessException("Client error: " + response.reason());
        }

        return defaultErrorDecoder.decode(methodKey, response);
    }
}
