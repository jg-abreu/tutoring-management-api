package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.config.ClockConfig;
import com.joaoguilherme.tutoringmanagementsystem.config.CorsConfig;
import com.joaoguilherme.tutoringmanagementsystem.config.SecurityConfig;
import com.joaoguilherme.tutoringmanagementsystem.model.Enrollment;
import com.joaoguilherme.tutoringmanagementsystem.model.Session;
import com.joaoguilherme.tutoringmanagementsystem.model.Subject;
import com.joaoguilherme.tutoringmanagementsystem.model.TutoringBond;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.EnrollmentStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;
import com.joaoguilherme.tutoringmanagementsystem.repository.UserRepository;
import com.joaoguilherme.tutoringmanagementsystem.service.SessionService;
import com.joaoguilherme.tutoringmanagementsystem.service.SubjectService;
import com.joaoguilherme.tutoringmanagementsystem.service.TokenService;
import com.joaoguilherme.tutoringmanagementsystem.service.TutoringBondService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.bond;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.enrollment;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.session;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.subject;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {SubjectController.class, TutoringBondController.class, SessionController.class, EnrollmentController.class},
        properties = "security.jwt.secret=test-secret-with-enough-length-for-hmac")
@Import({SecurityConfig.class, CorsConfig.class, TokenService.class, ClockConfig.class})
public class CurrentUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SubjectService subjectService;

    @MockitoBean
    private TutoringBondService tutoringBondService;

    @MockitoBean
    private SessionService sessionService;

    private User currentUser;
    private final UUID otherUserUuid = UUID.randomUUID();
    private final UUID resourceUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setEmail("student1@tutoring.dev");
        currentUser.setActive(true);
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        stubServiceResponses();
    }

    private void stubServiceResponses() {
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);
        Subject subject = subject(currentUser, admin);
        TutoringBond bond = bond(currentUser, admin, subject);
        Session session = session(bond);
        Enrollment enrollment = enrollment(session, currentUser, EnrollmentStatus.CONFIRMED, null);
        when(subjectService.suggestSubject(any(), anyString())).thenReturn(subject);
        when(subjectService.approveSubject(any(), any())).thenReturn(subject);
        when(subjectService.rejectSubject(any(), any())).thenReturn(subject);
        when(tutoringBondService.requestTutoringBond(any(), any())).thenReturn(bond);
        when(tutoringBondService.approveTutoringBond(any(), any())).thenReturn(bond);
        when(tutoringBondService.rejectTutoringBond(any(), any())).thenReturn(bond);
        when(tutoringBondService.revokeTutoringBond(any(), any())).thenReturn(bond);
        when(sessionService.createSession(any(), any(), any(), any(), anyInt(), anyBoolean())).thenReturn(session);
        when(sessionService.cancelSession(any(), any())).thenReturn(session);
        when(sessionService.cancelEnrollment(any(), any())).thenReturn(enrollment);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.generate(currentUser).token());
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return authenticated(request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void shouldSuggestSubjectAsTokenUserWhenBodyHasNoUserId() throws Exception {
        mockMvc.perform(json(post("/subjects"), "{\"subjectName\":\"Linear Algebra\"}"))
                .andExpect(status().isCreated());
        verify(subjectService).suggestSubject(currentUser.getId(), "Linear Algebra");
    }

    @Test
    void shouldIgnoreUserIdInBodyWhenSuggestingSubject() throws Exception {
        mockMvc.perform(json(post("/subjects"), "{\"userUuid\":\"" + otherUserUuid + "\",\"subjectName\":\"Linear Algebra\"}"))
                .andExpect(status().isCreated());
        verify(subjectService).suggestSubject(currentUser.getId(), "Linear Algebra");
    }

    @Test
    void shouldReturnBadRequestWhenSubjectNameIsBlank() throws Exception {
        mockMvc.perform(json(post("/subjects"), "{\"subjectName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldApproveSubjectAsTokenUserWithoutBody() throws Exception {
        mockMvc.perform(authenticated(patch("/subjects/{id}/approve", resourceUuid)))
                .andExpect(status().isOk());
        verify(subjectService).approveSubject(resourceUuid, currentUser.getId());
    }

    @Test
    void shouldRejectSubjectAsTokenUserWithoutBody() throws Exception {
        mockMvc.perform(authenticated(patch("/subjects/{id}/reject", resourceUuid)))
                .andExpect(status().isOk());
        verify(subjectService).rejectSubject(resourceUuid, currentUser.getId());
    }

    @Test
    void shouldRequestTutoringBondAsTokenUser() throws Exception {
        mockMvc.perform(json(post("/tutoring-bonds"), "{\"subjectUuid\":\"" + resourceUuid + "\",\"requesterUuid\":\"" + otherUserUuid + "\"}"))
                .andExpect(status().isCreated());
        verify(tutoringBondService).requestTutoringBond(resourceUuid, currentUser.getId());
    }

    @Test
    void shouldApproveTutoringBondAsTokenUserWithoutBody() throws Exception {
        mockMvc.perform(authenticated(patch("/tutoring-bonds/{id}/approve", resourceUuid)))
                .andExpect(status().isOk());
        verify(tutoringBondService).approveTutoringBond(resourceUuid, currentUser.getId());
    }

    @Test
    void shouldRejectTutoringBondAsTokenUserWithoutBody() throws Exception {
        mockMvc.perform(authenticated(patch("/tutoring-bonds/{id}/reject", resourceUuid)))
                .andExpect(status().isOk());
        verify(tutoringBondService).rejectTutoringBond(resourceUuid, currentUser.getId());
    }

    @Test
    void shouldRevokeTutoringBondAsTokenUserWithoutBody() throws Exception {
        mockMvc.perform(authenticated(patch("/tutoring-bonds/{id}/revoke", resourceUuid)))
                .andExpect(status().isOk());
        verify(tutoringBondService).revokeTutoringBond(resourceUuid, currentUser.getId());
    }

    @Test
    void shouldCreateSessionAsTokenUser() throws Exception {
        String body = "{\"tutoringBondUuid\":\"" + resourceUuid + "\",\"userUuid\":\"" + otherUserUuid + "\","
                + "\"startTime\":\"2026-12-01T14:00:00-03:00\",\"endTime\":\"2026-12-01T16:00:00-03:00\","
                + "\"spots\":2,\"allowsWaitlist\":true}";

        mockMvc.perform(json(post("/sessions"), body))
                .andExpect(status().isCreated());
        verify(sessionService).createSession(eq(resourceUuid), eq(currentUser.getId()),
                argThat(start -> start.isEqual(OffsetDateTime.parse("2026-12-01T14:00:00-03:00"))),
                argThat(end -> end.isEqual(OffsetDateTime.parse("2026-12-01T16:00:00-03:00"))), eq(2), eq(true));
    }

    @Test
    void shouldCancelSessionAsTokenUserWithoutBody() throws Exception {
        mockMvc.perform(authenticated(patch("/sessions/{id}/cancel", resourceUuid)))
                .andExpect(status().isOk());
        verify(sessionService).cancelSession(resourceUuid, currentUser.getId());
    }

    @Test
    void shouldCancelEnrollmentAsTokenUserWithoutBody() throws Exception {
        mockMvc.perform(authenticated(patch("/enrollments/{id}/cancel", resourceUuid)))
                .andExpect(status().isOk());
        verify(sessionService).cancelEnrollment(resourceUuid, currentUser.getId());
    }
}
