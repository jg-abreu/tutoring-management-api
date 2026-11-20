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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {SubjectController.class, TutoringBondController.class, SessionController.class, EnrollmentController.class},
        properties = "security.jwt.secret=test-secret-with-enough-length-for-hmac")
@Import({SecurityConfig.class, CorsConfig.class, TokenService.class, ClockConfig.class})
class ResponseBodyControllerTest {

    private static final List<String> HIDDEN_USER_FIELDS = List.of("passwordHash", "password", "authorities", "username",
            "enabled", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "active", "role", "createdAt");

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
    private User admin;
    private Subject subject;
    private TutoringBond bond;
    private Session session;
    private Enrollment enrollment;
    private final UUID resourceUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        currentUser = user("tutor@tutoring.dev", UserRole.USER);
        admin = user("admin@tutoring.dev", UserRole.ADMIN);
        subject = subject(currentUser, admin);
        bond = bond(currentUser, admin, subject);
        session = session(bond);
        enrollment = enrollment(session, user("student3@tutoring.dev", UserRole.USER), EnrollmentStatus.WAITLISTED, 1);
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.generate(currentUser).token());
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return authenticated(request).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static void expectUserSummary(ResultActions result, String path, User user) throws Exception {
        result.andExpect(jsonPath(path + ".id").value(user.getId().toString()))
                .andExpect(jsonPath(path + ".email").value(user.getEmail()));
        for (String field : HIDDEN_USER_FIELDS) {
            result.andExpect(jsonPath(path + "." + field).doesNotExist());
        }
    }

    private void expectSubject(ResultActions result, String path) throws Exception {
        result.andExpect(jsonPath(path + ".id").value(subject.getId().toString()))
                .andExpect(jsonPath(path + ".name").value("Calculus I"))
                .andExpect(jsonPath(path + ".status").value("APPROVED"))
                .andExpect(jsonPath(path + ".createdAt").exists());
        expectUserSummary(result, path + ".creator", currentUser);
        expectUserSummary(result, path + ".evaluator", admin);
    }

    private void expectBond(ResultActions result, String path) throws Exception {
        result.andExpect(jsonPath(path + ".id").value(bond.getId().toString()))
                .andExpect(jsonPath(path + ".status").value("APPROVED"))
                .andExpect(jsonPath(path + ".createdAt").exists());
        expectUserSummary(result, path + ".requester", currentUser);
        expectUserSummary(result, path + ".evaluator", admin);
        expectSubject(result, path + ".subject");
    }

    private void expectSession(ResultActions result, String path) throws Exception {
        result.andExpect(jsonPath(path + ".id").value(session.getId().toString()))
                .andExpect(jsonPath(path + ".startTime").exists())
                .andExpect(jsonPath(path + ".endTime").exists())
                .andExpect(jsonPath(path + ".spots").value(2))
                .andExpect(jsonPath(path + ".allowsWaitlist").value(true))
                .andExpect(jsonPath(path + ".status").value("ACTIVE"))
                .andExpect(jsonPath(path + ".createdAt").exists());
        expectBond(result, path + ".bond");
    }

    private void expectEnrollment(ResultActions result) throws Exception {
        result.andExpect(jsonPath("$.id").value(enrollment.getId().toString()))
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.queuePosition").value(1))
                .andExpect(jsonPath("$.createdAt").exists());
        expectUserSummary(result, "$.student", enrollment.getStudent());
        expectSession(result, "$.session");
    }

    @Test
    void shouldReturnSubjectResponseWhenSuggestingSubject() throws Exception {
        when(subjectService.suggestSubject(any(), anyString())).thenReturn(subject);

        ResultActions result = mockMvc.perform(json(post("/subjects"), "{\"subjectName\":\"Calculus I\"}"))
                .andExpect(status().isCreated());

        expectSubject(result, "$");
    }

    @Test
    void shouldReturnSubjectResponseWhenApprovingSubject() throws Exception {
        when(subjectService.approveSubject(any(), any())).thenReturn(subject);

        expectSubject(mockMvc.perform(authenticated(patch("/subjects/{id}/approve", resourceUuid)))
                .andExpect(status().isOk()), "$");
    }

    @Test
    void shouldReturnSubjectResponseWhenRejectingSubject() throws Exception {
        when(subjectService.rejectSubject(any(), any())).thenReturn(subject);

        expectSubject(mockMvc.perform(authenticated(patch("/subjects/{id}/reject", resourceUuid)))
                .andExpect(status().isOk()), "$");
    }

    @Test
    void shouldReturnNullEvaluatorWhenSubjectIsPending() throws Exception {
        when(subjectService.suggestSubject(any(), anyString())).thenReturn(subject(currentUser, null));

        mockMvc.perform(json(post("/subjects"), "{\"subjectName\":\"Calculus I\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evaluator").isEmpty());
    }

    @Test
    void shouldReturnTutoringBondResponseWhenRequestingBond() throws Exception {
        when(tutoringBondService.requestTutoringBond(any(), any())).thenReturn(bond);

        expectBond(mockMvc.perform(json(post("/tutoring-bonds"), "{\"subjectUuid\":\"" + resourceUuid + "\"}"))
                .andExpect(status().isCreated()), "$");
    }

    @Test
    void shouldReturnTutoringBondResponseWhenApprovingBond() throws Exception {
        when(tutoringBondService.approveTutoringBond(any(), any())).thenReturn(bond);

        expectBond(mockMvc.perform(authenticated(patch("/tutoring-bonds/{id}/approve", resourceUuid)))
                .andExpect(status().isOk()), "$");
    }

    @Test
    void shouldReturnTutoringBondResponseWhenRejectingBond() throws Exception {
        when(tutoringBondService.rejectTutoringBond(any(), any())).thenReturn(bond);

        expectBond(mockMvc.perform(authenticated(patch("/tutoring-bonds/{id}/reject", resourceUuid)))
                .andExpect(status().isOk()), "$");
    }

    @Test
    void shouldReturnTutoringBondResponseWhenRevokingBond() throws Exception {
        when(tutoringBondService.revokeTutoringBond(any(), any())).thenReturn(bond);

        expectBond(mockMvc.perform(authenticated(patch("/tutoring-bonds/{id}/revoke", resourceUuid)))
                .andExpect(status().isOk()), "$");
    }

    @Test
    void shouldReturnSessionResponseWhenCreatingSession() throws Exception {
        when(sessionService.createSession(any(), any(), any(), any(), anyInt(), anyBoolean())).thenReturn(session);
        String body = "{\"tutoringBondUuid\":\"" + resourceUuid + "\","
                + "\"startTime\":\"2026-12-01T14:00:00-03:00\",\"endTime\":\"2026-12-01T16:00:00-03:00\","
                + "\"spots\":2,\"allowsWaitlist\":true}";

        expectSession(mockMvc.perform(json(post("/sessions"), body)).andExpect(status().isCreated()), "$");
    }

    @Test
    void shouldReturnSessionResponseWhenCancellingSession() throws Exception {
        when(sessionService.cancelSession(any(), any())).thenReturn(session);

        expectSession(mockMvc.perform(authenticated(patch("/sessions/{id}/cancel", resourceUuid)))
                .andExpect(status().isOk()), "$");
    }

    @Test
    void shouldReturnEnrollmentResponseWhenEnrolling() throws Exception {
        when(sessionService.enroll(any(), any())).thenReturn(enrollment);

        expectEnrollment(mockMvc.perform(authenticated(post("/sessions/{id}/enrollments", resourceUuid)))
                .andExpect(status().isCreated()));
    }

    @Test
    void shouldReturnEnrollmentResponseWhenCancellingEnrollment() throws Exception {
        when(sessionService.cancelEnrollment(any(), any())).thenReturn(enrollment);

        expectEnrollment(mockMvc.perform(authenticated(patch("/enrollments/{id}/cancel", resourceUuid)))
                .andExpect(status().isOk()));
    }
}
