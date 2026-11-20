package com.joaoguilherme.tutoringmanagementsystem.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.RegisteredClaims;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidTokenException;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class TokenService {

    private static final String ISSUER = "tutoring-management-system";

    private final Algorithm algorithm;
    private final Duration expiration;
    private final Clock clock;
    private final JWTVerifier verifier;

    public TokenService(@Value("${security.jwt.secret}") String secret,
                        @Value("${security.jwt.expiration}") Duration expiration,
                        Clock clock) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expiration = expiration;
        this.clock = clock;
        this.verifier = ((JWTVerifier.BaseVerification) JWT.require(algorithm).withIssuer(ISSUER).withClaimPresence(RegisteredClaims.SUBJECT)).build(clock);
    }

    public IssuedToken generate(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(expiration);
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getId().toString())
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
        return new IssuedToken(token, expiresAt);
    }

    public UUID validate(String token) {
        try {
            return UUID.fromString(verifier.verify(token).getSubject());
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
