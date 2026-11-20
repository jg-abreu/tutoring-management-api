package com.joaoguilherme.tutoringmanagementsystem.service;

import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidTokenException;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

public class TokenServiceTest {

    private static final String SECRET = "test-secret-with-enough-length-for-hmac";
    private static final Duration EXPIRATION = Duration.ofHours(2);
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private TokenService tokenServiceAt(Instant instant, String secret) {
        return new TokenService(secret, EXPIRATION, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private User newUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void shouldReturnUserIdWhenTokenIsValid() {
        TokenService tokenService = tokenServiceAt(NOW, SECRET);
        User user = newUser();

        String token = tokenService.generate(user).token();

        Assertions.assertEquals(user.getId(), tokenService.validate(token));
    }

    @Test
    void shouldExpireTwoHoursAfterIssueWhenGeneratingToken() {
        TokenService tokenService = tokenServiceAt(NOW, SECRET);

        TokenService.IssuedToken issuedToken = tokenService.generate(newUser());

        Assertions.assertEquals(NOW.plus(EXPIRATION), issuedToken.expiresAt());
    }

    @Test
    void shouldAcceptTokenWhenItIsAboutToExpire() {
        User user = newUser();
        String token = tokenServiceAt(NOW, SECRET).generate(user).token();

        UUID userId = tokenServiceAt(NOW.plus(EXPIRATION).minusSeconds(1), SECRET).validate(token);

        Assertions.assertEquals(user.getId(), userId);
    }

    @Test
    void shouldThrowInvalidTokenExceptionWhenTokenIsExpired() {
        String token = tokenServiceAt(NOW, SECRET).generate(newUser()).token();
        TokenService later = tokenServiceAt(NOW.plus(EXPIRATION).plusSeconds(1), SECRET);

        Assertions.assertThrows(InvalidTokenException.class, () -> later.validate(token));
    }

    @Test
    void shouldThrowInvalidTokenExceptionWhenPayloadIsTampered() {
        TokenService tokenService = tokenServiceAt(NOW, SECRET);
        String token = tokenService.generate(newUser()).token();
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"tutoring-management-system\",\"sub\":\"" + UUID.randomUUID()
                        + "\",\"exp\":" + NOW.plus(EXPIRATION).getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        Assertions.assertThrows(InvalidTokenException.class, () -> tokenService.validate(tampered));
    }

    @Test
    void shouldThrowInvalidTokenExceptionWhenSignedWithAnotherSecret() {
        String token = tokenServiceAt(NOW, "another-secret-with-enough-length-for-hmac").generate(newUser()).token();
        TokenService tokenService = tokenServiceAt(NOW, SECRET);

        Assertions.assertThrows(InvalidTokenException.class, () -> tokenService.validate(token));
    }

    @Test
    void shouldThrowInvalidTokenExceptionWhenTokenIsMalformed() {
        TokenService tokenService = tokenServiceAt(NOW, SECRET);

        Assertions.assertThrows(InvalidTokenException.class, () -> tokenService.validate("not-a-jwt"));
    }
}
