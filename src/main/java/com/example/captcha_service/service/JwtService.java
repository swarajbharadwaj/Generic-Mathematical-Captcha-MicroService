package com.example.captcha_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long ttlInMillis;

    public JwtService(
            @Value("${captcha.jwt.secret}") String secret,
            @Value("${captcha.jwt.ttl-seconds}") long ttlSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlInMillis = ttlSeconds * 1000;
    }

    /**
     * Generates a JWT with the expected answer embedded as a claim.
     * @param expectedAnswer The correct answer to the CAPTCHA.
     * @return A signed JWT string.
     */
    public String generateToken(String expectedAnswer) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlInMillis);

        return Jwts.builder()
                .subject("captcha-answer")
                .claim("answer", expectedAnswer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates the JWT and extracts the embedded answer.
     * @param token The JWT string from the client.
     * @return An Optional containing the correct answer if the token is valid, otherwise empty.
     */
    public Optional<String> validateAndGetAnswer(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.ofNullable(claims.get("answer", String.class));
        } catch (Exception e) {
            // Catches ExpiredJwtException, MalformedJwtException, SignatureException etc.
            return Optional.empty();
        }
    }
}
