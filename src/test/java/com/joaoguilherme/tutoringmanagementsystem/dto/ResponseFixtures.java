package com.joaoguilherme.tutoringmanagementsystem.dto;

import com.joaoguilherme.tutoringmanagementsystem.model.Enrollment;
import com.joaoguilherme.tutoringmanagementsystem.model.Session;
import com.joaoguilherme.tutoringmanagementsystem.model.Subject;
import com.joaoguilherme.tutoringmanagementsystem.model.TutoringBond;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.BondStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.EnrollmentStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SubjectStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ResponseFixtures {

    public static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-11-20T10:00:00Z");

    public static User user(String email, UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hash");
        user.setRole(role);
        user.setCreatedAt(CREATED_AT);
        return user;
    }

    public static Subject subject(User creator, User evaluator) {
        Subject subject = new Subject(creator, "Calculus I");
        subject.setId(UUID.randomUUID());
        subject.setStatus(evaluator == null ? SubjectStatus.PENDING : SubjectStatus.APPROVED);
        subject.setEvaluator(evaluator);
        subject.setCreatedAt(CREATED_AT);
        return subject;
    }

    public static TutoringBond bond(User requester, User evaluator, Subject subject) {
        TutoringBond bond = new TutoringBond(requester, subject);
        bond.setId(UUID.randomUUID());
        bond.setStatus(evaluator == null ? BondStatus.PENDING : BondStatus.APPROVED);
        bond.setEvaluator(evaluator);
        bond.setCreatedAt(CREATED_AT);
        return bond;
    }

    public static Session session(TutoringBond bond) {
        Session session = new Session(CREATED_AT.plusDays(1), CREATED_AT.plusDays(1).plusHours(2), 2, true, bond);
        session.setId(UUID.randomUUID());
        session.setCreatedAt(CREATED_AT);
        return session;
    }

    public static Enrollment enrollment(Session session, User student, EnrollmentStatus status, Integer queuePosition) {
        Enrollment enrollment = new Enrollment(status, session, student);
        enrollment.setId(UUID.randomUUID());
        enrollment.setQueuePosition(queuePosition);
        enrollment.setCreatedAt(CREATED_AT);
        return enrollment;
    }

    private ResponseFixtures() {
    }
}
