package com.bank.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;

public class SecurityUtil
{
    public static Claims getClaims(SecretKey jwtSecret, String token)
    {
        return Jwts.parser()
            .verifyWith(jwtSecret)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean isTokenExpired(SecretKey jwtSecret, String token) {
        try {
            Claims claims = getClaims(jwtSecret, token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true; // Treat invalid tokens as expired
        }
    }
}
