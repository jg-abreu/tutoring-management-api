package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.config.ClockConfig;
import com.joaoguilherme.tutoringmanagementsystem.config.CorsConfig;
import com.joaoguilherme.tutoringmanagementsystem.config.SecurityConfig;
import com.joaoguilherme.tutoringmanagementsystem.model.Session;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.EnrollmentStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;
import com.joaoguilherme.tutoringmanagementsystem.repository.UserRepository;
import com.joaoguilherme.tutoringmanagementsystem.service.SessionService;
import com.joaoguilherme.tutoringmanagementsystem.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.bond;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.enrollment;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.session;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.subject;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.user;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SessionController.class, properties = "security.jwt.secret=" + SessionControllerSecurityTest.SECRET)
@Import({SecurityConfig.class, CorsConfig.class, TokenService.class, ClockConfig.class})
public class SessionControllerSecurityTest {

    static final String SECRET = "test-secret-with-enough-length-for-hmac";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SessionService sessionService;

    private final UUID sessionUuid = UUID.randomUUID();

    private User activeUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("student1@tutoring.dev");
        user.setActive(true);
        return user;
    }

    private MockHttpServletRequestBuilder enroll() {
        return post("/sessions/{sessionUuid}/enrollments", sessionUuid);
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(enroll())
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldReturnUnauthorizedWhenHeaderIsNotBearer() throws Exception {
        mockMvc.perform(enroll().header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsExpired() throws Exception {
        User user = activeUser();
        Instant threeHoursAgo = Instant.now().minus(Duration.ofHours(3));
        TokenService pastTokenService = new TokenService(SECRET, Duration.ofHours(2), Clock.fixed(threeHoursAgo, ZoneOffset.UTC));
        String expiredToken = pastTokenService.generate(user).token();

        mockMvc.perform(enroll().header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsTampered() throws Exception {
        User user = activeUser();
        String[] parts = tokenService.generate(user).token().split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        String forged = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.replace(user.getId().toString(), UUID.randomUUID().toString()).getBytes());

        mockMvc.perform(enroll().header(HttpHeaders.AUTHORIZATION, "Bearer " + parts[0] + "." + forged + "." + parts[2]))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldReturnUnauthorizedWhenUserNoLongerExists() throws Exception {
        User user = activeUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        mockMvc.perform(enroll().header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.generate(user).token()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsInactive() throws Exception {
        User user = activeUser();
        user.setActive(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        mockMvc.perform(enroll().header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.generate(user).token()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldEnrollTheTokenUserWhenTokenIsValid() throws Exception {
        User user = activeUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = session(bond(user, admin, subject(user, admin)));
        when(sessionService.enroll(sessionUuid, user.getId())).thenReturn(enrollment(session, user, EnrollmentStatus.CONFIRMED, null));

        mockMvc.perform(enroll().header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.generate(user).token()))
                .andExpect(status().isCreated());
        verify(sessionService).enroll(sessionUuid, user.getId());
    }

    @Test
    void shouldAllowCorsPreflightFromDevServerWithoutToken() throws Exception {
        mockMvc.perform(options("/sessions/{sessionUuid}/enrollments", sessionUuid)
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
