package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.config.ClockConfig;
import com.joaoguilherme.tutoringmanagementsystem.config.CorsConfig;
import com.joaoguilherme.tutoringmanagementsystem.config.SecurityConfig;
import com.joaoguilherme.tutoringmanagementsystem.exception.SessionNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.SubjectNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.TutoringBondNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.UserWithoutPermissionException;
import com.joaoguilherme.tutoringmanagementsystem.model.Enrollment;
import com.joaoguilherme.tutoringmanagementsystem.model.Session;
import com.joaoguilherme.tutoringmanagementsystem.model.Subject;
import com.joaoguilherme.tutoringmanagementsystem.model.TutoringBond;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.BondStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.EnrollmentStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SessionStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SubjectStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;
import com.joaoguilherme.tutoringmanagementsystem.repository.UserRepository;
import com.joaoguilherme.tutoringmanagementsystem.service.SessionService;
import com.joaoguilherme.tutoringmanagementsystem.service.SubjectService;
import com.joaoguilherme.tutoringmanagementsystem.service.TokenService;
import com.joaoguilherme.tutoringmanagementsystem.service.TutoringBondService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {SubjectController.class, TutoringBondController.class, SessionController.class, EnrollmentController.class},
        properties = "security.jwt.secret=test-secret-with-enough-length-for-hmac")
@Import({SecurityConfig.class, CorsConfig.class, TokenService.class, ClockConfig.class})
class ReadEndpointsControllerTest {

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
    private Subject subject;
    private TutoringBond bond;
    private Session session;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        currentUser = user("student1@tutoring.dev", UserRole.USER);
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);
        subject = subject(currentUser, admin);
        bond = bond(currentUser, admin, subject);
        session = session(bond);
        enrollment = enrollment(session, currentUser, EnrollmentStatus.CONFIRMED, null);
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.generate(currentUser).token());
    }

    @Test
    void shouldListSubjectsWithoutFilterWhenNoStatusIsGiven() throws Exception {
        when(subjectService.listSubjects(null)).thenReturn(List.of(subject));

        mockMvc.perform(authenticated(get("/subjects")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(subject.getId().toString()))
                .andExpect(jsonPath("$[0].creator.email").value("student1@tutoring.dev"))
                .andExpect(jsonPath("$[0].creator.role").doesNotExist());
    }

    @Test
    void shouldPassStatusToServiceWhenListingSubjectsWithFilter() throws Exception {
        when(subjectService.listSubjects(SubjectStatus.APPROVED)).thenReturn(List.of(subject));

        mockMvc.perform(authenticated(get("/subjects").param("status", "APPROVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(subjectService).listSubjects(SubjectStatus.APPROVED);
    }

    @Test
    void shouldReturnSubjectWhenReadingById() throws Exception {
        when(subjectService.findSubject(subject.getId())).thenReturn(subject);

        mockMvc.perform(authenticated(get("/subjects/{id}", subject.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Calculus I"));
    }

    @Test
    void shouldReturnNotFoundWhenSubjectDoesNotExist() throws Exception {
        UUID missing = UUID.randomUUID();
        when(subjectService.findSubject(missing)).thenThrow(new SubjectNotFoundException("Subject not found"));

        mockMvc.perform(authenticated(get("/subjects/{id}", missing)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListTutoringBondsAsTokenUserWithStatus() throws Exception {
        when(tutoringBondService.listTutoringBonds(currentUser.getId(), BondStatus.APPROVED)).thenReturn(List.of(bond));

        mockMvc.perform(authenticated(get("/tutoring-bonds").param("status", "APPROVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bond.getId().toString()))
                .andExpect(jsonPath("$[0].subject.name").value("Calculus I"));
        verify(tutoringBondService).listTutoringBonds(currentUser.getId(), BondStatus.APPROVED);
    }

    @Test
    void shouldReadTutoringBondAsTokenUser() throws Exception {
        when(tutoringBondService.findTutoringBond(bond.getId(), currentUser.getId())).thenReturn(bond);

        mockMvc.perform(authenticated(get("/tutoring-bonds/{id}", bond.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requester.id").value(currentUser.getId().toString()));
    }

    @Test
    void shouldReturnForbiddenWhenReadingAnotherUsersBond() throws Exception {
        UUID other = UUID.randomUUID();
        when(tutoringBondService.findTutoringBond(other, currentUser.getId())).thenThrow(new UserWithoutPermissionException("User without permission"));

        mockMvc.perform(authenticated(get("/tutoring-bonds/{id}", other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenTutoringBondDoesNotExist() throws Exception {
        UUID missing = UUID.randomUUID();
        when(tutoringBondService.findTutoringBond(missing, currentUser.getId())).thenThrow(new TutoringBondNotFoundException("TutoringBond not found"));

        mockMvc.perform(authenticated(get("/tutoring-bonds/{id}", missing)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListSessionsWithStatus() throws Exception {
        when(sessionService.listSessions(SessionStatus.ACTIVE)).thenReturn(List.of(session));

        mockMvc.perform(authenticated(get("/sessions").param("status", "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(session.getId().toString()))
                .andExpect(jsonPath("$[0].bond.subject.name").value("Calculus I"));
    }

    @Test
    void shouldReturnSessionWhenReadingById() throws Exception {
        when(sessionService.findSession(session.getId())).thenReturn(session);

        mockMvc.perform(authenticated(get("/sessions/{id}", session.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spots").value(2));
    }

    @Test
    void shouldListSessionEnrollmentsInServiceOrder() throws Exception {
        Enrollment waiting = enrollment(session, user("student2@tutoring.dev", UserRole.USER), EnrollmentStatus.WAITLISTED, 1);
        when(sessionService.listSessionEnrollments(session.getId())).thenReturn(List.of(enrollment, waiting));

        mockMvc.perform(authenticated(get("/sessions/{id}/enrollments", session.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[1].status").value("WAITLISTED"))
                .andExpect(jsonPath("$[1].queuePosition").value(1))
                .andExpect(jsonPath("$[1].student.email").value("student2@tutoring.dev"));
    }

    @Test
    void shouldReturnNotFoundWhenListingEnrollmentsOfMissingSession() throws Exception {
        UUID missing = UUID.randomUUID();
        when(sessionService.listSessionEnrollments(missing)).thenThrow(new SessionNotFoundException("Session not found"));

        mockMvc.perform(authenticated(get("/sessions/{id}/enrollments", missing)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListOwnEnrollmentsAsTokenUser() throws Exception {
        when(sessionService.listUserEnrollments(currentUser.getId())).thenReturn(List.of(enrollment));

        mockMvc.perform(authenticated(get("/enrollments")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].student.id").value(currentUser.getId().toString()));
        verify(sessionService).listUserEnrollments(currentUser.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/subjects", "/tutoring-bonds", "/sessions"})
    void shouldReturnBadRequestWhenStatusIsUnknown(String path) throws Exception {
        mockMvc.perform(authenticated(get(path).param("status", "UNKNOWN")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(subjectService, tutoringBondService, sessionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/subjects/not-a-uuid", "/tutoring-bonds/not-a-uuid", "/sessions/not-a-uuid", "/sessions/not-a-uuid/enrollments"})
    void shouldReturnBadRequestWhenIdIsNotAUuid(String path) throws Exception {
        mockMvc.perform(authenticated(get(path)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(subjectService, tutoringBondService, sessionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/subjects", "/tutoring-bonds", "/sessions", "/enrollments"})
    void shouldReturnUnauthorizedWhenReadingWithoutToken(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(subjectService, tutoringBondService, sessionService);
    }

    @Test
    void shouldNotCallServiceWithAnotherUserWhenListingBonds() throws Exception {
        when(tutoringBondService.listTutoringBonds(any(), any())).thenReturn(List.of());

        mockMvc.perform(authenticated(get("/tutoring-bonds")))
                .andExpect(status().isOk());
        verify(tutoringBondService).listTutoringBonds(currentUser.getId(), null);
    }
}
