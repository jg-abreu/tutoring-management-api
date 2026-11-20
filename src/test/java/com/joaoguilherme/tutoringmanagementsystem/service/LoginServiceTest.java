package com.joaoguilherme.tutoringmanagementsystem.service;

import com.joaoguilherme.tutoringmanagementsystem.dto.LoginResponse;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private LoginService loginService;

    private User newUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student1@tutoring.dev");
        return user;
    }

    @Test
    void shouldReturnBearerTokenAndRoleWhenCredentialsAreValid() {
        User user = newUser();
        user.setRole(UserRole.ADMIN);
        Instant expiresAt = Instant.parse("2026-11-19T14:00:00Z");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        when(tokenService.generate(user)).thenReturn(new TokenService.IssuedToken("signed-token", expiresAt));

        LoginResponse response = loginService.login("student1@tutoring.dev", "senha123");

        Assertions.assertEquals("signed-token", response.token());
        Assertions.assertEquals("Bearer", response.tokenType());
        Assertions.assertEquals(expiresAt, response.expiresAt());
        Assertions.assertEquals(UserRole.ADMIN, response.role());
    }

    @Test
    void shouldThrowBadCredentialsExceptionWhenPasswordIsWrong() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        Assertions.assertThrows(BadCredentialsException.class,
                () -> loginService.login("student1@tutoring.dev", "wrong-password"));
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldThrowDisabledExceptionWhenUserIsInactive() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        Assertions.assertThrows(DisabledException.class,
                () -> loginService.login("inactive@tutoring.dev", "senha123"));
        verifyNoInteractions(tokenService);
    }
}
