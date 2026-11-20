package com.joaoguilherme.tutoringmanagementsystem.dto;

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
import org.junit.jupiter.api.Test;

import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.CREATED_AT;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.bond;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.enrollment;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.session;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.subject;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResponseMappingTest {

    @Test
    void shouldKeepOnlyIdAndEmailWhenMappingUser() {
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);

        UserSummary summary = UserSummary.from(admin);

        assertEquals(new UserSummary(admin.getId(), "admin@tutoring.dev"), summary);
    }

    @Test
    void shouldCopyFieldsAndSummarizeUsersWhenMappingSubject() {
        User creator = user("student1@tutoring.dev", UserRole.USER);
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);
        Subject subject = subject(creator, admin);

        SubjectResponse response = SubjectResponse.from(subject);

        assertEquals(subject.getId(), response.id());
        assertEquals("Calculus I", response.name());
        assertEquals(SubjectStatus.APPROVED, response.status());
        assertEquals(UserSummary.from(creator), response.creator());
        assertEquals(UserSummary.from(admin), response.evaluator());
        assertEquals(CREATED_AT, response.createdAt());
    }

    @Test
    void shouldMapEvaluatorToNullWhenSubjectIsPending() {
        Subject subject = subject(user("student1@tutoring.dev", UserRole.USER), null);

        assertNull(SubjectResponse.from(subject).evaluator());
    }

    @Test
    void shouldNestSubjectWhenMappingTutoringBond() {
        User tutor = user("tutor@tutoring.dev", UserRole.USER);
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);
        Subject subject = subject(tutor, admin);
        TutoringBond bond = bond(tutor, admin, subject);

        TutoringBondResponse response = TutoringBondResponse.from(bond);

        assertEquals(bond.getId(), response.id());
        assertEquals(BondStatus.APPROVED, response.status());
        assertEquals(UserSummary.from(tutor), response.requester());
        assertEquals(UserSummary.from(admin), response.evaluator());
        assertEquals(SubjectResponse.from(subject), response.subject());
        assertEquals(CREATED_AT, response.createdAt());
    }

    @Test
    void shouldMapEvaluatorToNullWhenTutoringBondIsPending() {
        User tutor = user("tutor@tutoring.dev", UserRole.USER);
        TutoringBond bond = bond(tutor, null, subject(tutor, null));

        assertNull(TutoringBondResponse.from(bond).evaluator());
    }

    @Test
    void shouldNestTutoringBondWhenMappingSession() {
        User tutor = user("tutor@tutoring.dev", UserRole.USER);
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);
        TutoringBond bond = bond(tutor, admin, subject(tutor, admin));
        Session session = session(bond);

        SessionResponse response = SessionResponse.from(session);

        assertEquals(session.getId(), response.id());
        assertEquals(session.getStartTime(), response.startTime());
        assertEquals(session.getEndTime(), response.endTime());
        assertEquals(2, response.spots());
        assertEquals(true, response.allowsWaitlist());
        assertEquals(SessionStatus.ACTIVE, response.status());
        assertEquals(TutoringBondResponse.from(bond), response.bond());
        assertEquals(CREATED_AT, response.createdAt());
    }

    @Test
    void shouldNestSessionAndSummarizeStudentWhenMappingEnrollment() {
        User tutor = user("tutor@tutoring.dev", UserRole.USER);
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);
        User student = user("student3@tutoring.dev", UserRole.USER);
        Session session = session(bond(tutor, admin, subject(tutor, admin)));
        Enrollment enrollment = enrollment(session, student, EnrollmentStatus.WAITLISTED, 1);

        EnrollmentResponse response = EnrollmentResponse.from(enrollment);

        assertEquals(enrollment.getId(), response.id());
        assertEquals(EnrollmentStatus.WAITLISTED, response.status());
        assertEquals(1, response.queuePosition());
        assertEquals(SessionResponse.from(session), response.session());
        assertEquals(UserSummary.from(student), response.student());
        assertEquals(CREATED_AT, response.createdAt());
    }

    @Test
    void shouldMapQueuePositionToNullWhenEnrollmentIsConfirmed() {
        User tutor = user("tutor@tutoring.dev", UserRole.USER);
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = session(bond(tutor, admin, subject(tutor, admin)));
        Enrollment enrollment = enrollment(session, user("student1@tutoring.dev", UserRole.USER), EnrollmentStatus.CONFIRMED, null);

        assertNull(EnrollmentResponse.from(enrollment).queuePosition());
    }
}
