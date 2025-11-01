/**
 * Security Classification: Confidential Copyright (c) Yunex Limited 2025. This is an unpublished work, with copyright
 * vested in Yunex Limited. All rights reserved. The information contained herein is the property of Yunex Limited and
 * is provided without liability for any errors or omissions. No part of this document may be copied, reproduced, used,
 * or disclosed except as authorized by contract or with prior written permission. The copyright and the restrictions on
 * reproduction, use, and disclosure apply to all media in which this information may be embodied. Where any information
 * is attributed to individual authors, the views expressed do not necessarily reflect the views of Yunex Limited.
 */
package com.bank.common.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class CustomErrorDecoder implements ErrorDecoder
{

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
