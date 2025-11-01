package com.bank.auth.service;

import com.bank.auth.entity.RefreshToken;
import com.bank.auth.repository.RefreshTokenRepository;
import com.bank.auth.repository.UserRepository;
import com.bank.common.dto.contracts.auth.TokenResponse;
import com.bank.common.dto.contracts.auth.TokenValidationResponse;
import com.bank.common.dto.contracts.auth.UserDTO;
import com.bank.common.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String USER_SCOPE = "user";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.access-token-validity:3600}")
    private long accessTokenValiditySeconds;

    @Value("${jwt.refresh-token-validity:86400}")
    private long refreshTokenValiditySeconds;

    @Value("${security.issuer}")
    private String issuer;

    public TokenResponse createTokenResponse(UserDTO user, boolean rememberMe) {
        Instant issuedAt = Instant.now();
        Instant accessExpiresAt = issuedAt.plusSeconds(accessTokenValiditySeconds);
        long refreshTtl = rememberMe ? refreshTokenValiditySeconds * 7 : refreshTokenValiditySeconds;
        Instant refreshExpiresAt = issuedAt.plusSeconds(refreshTtl);

        String scope = USER_SCOPE;

        String accessToken = generateAccessToken(user, issuedAt, accessExpiresAt, scope);
        String refreshToken = generateRefreshToken();

        refreshTokenRepository.deleteByUserId(user.getId());
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setUserId(user.getId());
        refreshTokenEntity.setExpiryDate(refreshExpiresAt);
        refreshTokenEntity.setCreatedAt(issuedAt);
        refreshTokenRepository.save(refreshTokenEntity);

        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(accessTokenValiditySeconds)
            .scope(scope)
            .build();
    }

    public TokenResponse createTokenResponse(UserDTO user) {
        return createTokenResponse(user, false);
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new InvalidTokenException("Refresh token expired");
        }

        com.bank.auth.entity.User user = userRepository.findById(storedToken.getUserId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Instant issuedAt = Instant.now();
        Instant accessExpiresAt = issuedAt.plusSeconds(accessTokenValiditySeconds);
        String scope = USER_SCOPE;

        String accessToken = generateAccessToken(mapUserToDto(user), issuedAt, accessExpiresAt, scope);

        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(accessTokenValiditySeconds)
            .scope(scope)
            .build();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public TokenValidationResponse validateToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Object userIdClaim = jwt.getClaim("user_id");
            Long userId = null;
            if (userIdClaim instanceof Number number) {
                userId = number.longValue();
            }

            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                roles = List.of();
            }

            return TokenValidationResponse.builder()
                .valid(true)
                .username(jwt.getSubject())
                .userId(userId)
                .roles(roles)
                .build();
        } catch (JwtException ex) {
            log.warn("Token validation failed: {}", ex.getMessage());
            return TokenValidationResponse.builder()
                .valid(false)
                .build();
        }
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
    }

    private String generateAccessToken(UserDTO user, Instant issuedAt, Instant expiresAt, String scope) {
        Set<String> roles = user.getRoles();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.getUsername())
            .claim("user_id", user.getId())
            .claim("roles", roles)
            .claim("scope", scope)
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String generateRefreshToken() {
        byte[] random = new byte[64];
        RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private UserDTO mapUserToDto(com.bank.auth.entity.User user) {
        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .roles(user.getRoles())
            .enabled(user.isEnabled())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}
