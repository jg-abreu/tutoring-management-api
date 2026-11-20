package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.config.SecurityConfig;
import com.joaoguilherme.tutoringmanagementsystem.dto.LoginResponse;
import com.joaoguilherme.tutoringmanagementsystem.repository.UserRepository;
import com.joaoguilherme.tutoringmanagementsystem.service.LoginService;
import com.joaoguilherme.tutoringmanagementsystem.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    private static String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void shouldReturnTokenWhenCredentialsAreValid() throws Exception {
        when(loginService.login("student1@tutoring.dev", "senha123"))
                .thenReturn(new LoginResponse("signed-token", "Bearer", Instant.parse("2026-11-19T14:00:00Z")));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("student1@tutoring.dev", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-19T14:00:00Z"));
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
        when(loginService.login(anyString(), anyString())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("student1@tutoring.dev", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    void shouldReturnSameUnauthorizedMessageWhenUserIsInactive() throws Exception {
        when(loginService.login(anyString(), anyString())).thenThrow(new DisabledException("User is disabled"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("inactive@tutoring.dev", "senha123")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    void shouldReturnBadRequestWhenEmailIsBlank() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "senha123")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(loginService);
    }

    @Test
    void shouldReturnBadRequestWhenEmailIsMalformed() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-an-email", "senha123")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(loginService);
    }

    @Test
    void shouldReturnBadRequestWhenPasswordIsBlank() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body("student1@tutoring.dev", "")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(loginService);
    }
}
