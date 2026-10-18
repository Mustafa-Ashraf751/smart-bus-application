package com.verysmartbus.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret must decode to at least 32 bytes.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * The extra claims are for the frontend only (showing the user, choosing screens).
     * The backend never trusts them: each request reloads the user, roles and permissions by "sub".
     */
    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(principal.getUserId().toString())
                .claim("userId", principal.getUserId())
                .claim("email", principal.getUsername())
                .claim("username", principal.getLoginUsername())
                .claim("name", principal.getName())
                .claim("phone", principal.getPhone())
                .claim("roles", principal.getRoleNames())
                .claim("permissions", principal.getPermissionNames())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(extractClaims(token).getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("JWT validation failed: {}", exception.getMessage());
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1_000;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
