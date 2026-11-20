package com.joaoguilherme.tutoringmanagementsystem.service;

import com.joaoguilherme.tutoringmanagementsystem.exception.*;
import com.joaoguilherme.tutoringmanagementsystem.model.*;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.BondStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.EnrollmentStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.NotificationEventType;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SessionStatus;
import com.joaoguilherme.tutoringmanagementsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final DateTimeFormatter NOTIFICATION_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private static final Comparator<Enrollment> SESSION_ENROLLMENT_ORDER = Comparator.comparingInt(SessionService::statusRank)
            .thenComparingInt(SessionService::queueRank)
            .thenComparing(Enrollment::getCreatedAt);

    private final TutoringBondRepository tutoringBondRepository;
    private final SessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public Session createSession(UUID tutoringBondUuid, UUID userId, OffsetDateTime startTime, OffsetDateTime endTime, int spots, boolean allowsWaitList) {
        if (!(endTime.isAfter(startTime))) {
            throw new InvalidTimeException("Invalid Time Exception");
        }

        TutoringBond tutoringBond = tutoringBondRepository.findById(tutoringBondUuid).orElseThrow(() -> new TutoringBondNotFoundException("TutoringBond not found"));

        if (!(isRequesterOrEvaluator(tutoringBond, userId))) {
            throw new UserWithoutPermissionException("User without permission");
        }

        if (tutoringBond.getStatus() != BondStatus.APPROVED) {
            throw new InvalidTutoringBondStatusException("Invalid tutoringBond status exception");
        }

        Session session = new Session(startTime, endTime, spots, allowsWaitList, tutoringBond);

        return sessionRepository.save(session);
    }

    @Transactional
    public Session cancelSession(UUID sessionUuid, UUID userUuid) {
        Session session = sessionRepository.findById(sessionUuid).orElseThrow(() -> new SessionNotFoundException("Session not found"));

        if (!(isEvaluator(session.getBond(), userUuid))) {
            throw new UserWithoutPermissionException("User without permission");
        }

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InvalidSessionStatusException("Invalid Session Status Exception");
        }

        session.setStatus(SessionStatus.CANCELLED);
        Session cancelled = sessionRepository.save(session);
        cancelActiveEnrollments(cancelled, "");
        return cancelled;
    }

    @Transactional
    public List<Session> cancelFutureSessions(TutoringBond tutoringBond) {
        OffsetDateTime presentMoment = OffsetDateTime.now();

        List<Session> futureSessions = sessionRepository.findByBondAndStatusAndStartTimeAfter(tutoringBond, SessionStatus.ACTIVE, presentMoment);

        futureSessions.forEach(s -> s.setStatus(SessionStatus.CANCELLED));

        List<Session> cancelled = sessionRepository.saveAll(futureSessions);
        cancelled.forEach(session -> cancelActiveEnrollments(session, " because the tutor's bond was revoked"));
        return cancelled;
    }

    public Enrollment enroll(UUID sessionUuid, UUID studentUuid) {
        Session session = sessionRepository.findById(sessionUuid).orElseThrow(() -> new SessionNotFoundException("Session not found"));

        User student = userRepository.findById(studentUuid).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InvalidSessionStatusException("Invalid session status");
        }

        boolean isAlreadyEnrolled = enrollmentRepository.existsBySessionAndStudentAndStatusIn(session, student, List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED));

        if (isAlreadyEnrolled) {
            throw new EnrollmentAlreadyExistsException("Enrollment already exists");
        }

        long confirmedCount = enrollmentRepository.countBySessionAndStatus(session, EnrollmentStatus.CONFIRMED);

        if (confirmedCount < session.getSpots()) {
            Enrollment enrollment = new Enrollment(EnrollmentStatus.CONFIRMED, session, student);
            return enrollmentRepository.save(enrollment);
        }

        if (session.isAllowsWaitlist()) {
            long waitlistedCount = enrollmentRepository.countBySessionAndStatus(session, EnrollmentStatus.WAITLISTED);

            int queuePosition = (int) waitlistedCount + 1;

            Enrollment enrollment = new Enrollment(EnrollmentStatus.WAITLISTED, session, student);

            enrollment.setQueuePosition(queuePosition);

            return enrollmentRepository.save(enrollment);
        }

        throw new SessionFullException("Session is full");
    }

    @Transactional
    public Enrollment cancelEnrollment(UUID enrollmentUuid, UUID userUuid) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentUuid).orElseThrow(() -> new EnrollmentNotFoundException("Enrollment not found"));

        if (!(enrollment.getStudent().getId().equals(userUuid))) {
            throw new UserWithoutPermissionException("User without permission");
        }

        if (enrollment.getStatus() != EnrollmentStatus.CONFIRMED && enrollment.getStatus() != EnrollmentStatus.WAITLISTED) {
            throw new InvalidEnrollmentStatusException("Invalid enrollment status");
        }

        EnrollmentStatus previousStatus = enrollment.getStatus();

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollment.setQueuePosition(null);

        List<Enrollment> waitlist = enrollmentRepository.findBySessionAndStatusOrderByQueuePositionAsc(enrollment.getSession(), EnrollmentStatus.WAITLISTED);

        if (previousStatus == EnrollmentStatus.CONFIRMED && !waitlist.isEmpty()) {
            Enrollment promoted = waitlist.get(0);
            promoted.setStatus(EnrollmentStatus.CONFIRMED);
            promoted.setQueuePosition(null);
            Notification notification = new Notification(NotificationEventType.SPOT_PROMOTED, "You have been promoted from the waitlist", promoted.getStudent());
            notificationRepository.save(notification);
        }

        int position = 1;

        for (Enrollment waiting : waitlist) {
            if (waiting.getStatus() == EnrollmentStatus.WAITLISTED) {
                waiting.setQueuePosition(position);
                position++;
            }
        }

        enrollmentRepository.saveAll(waitlist);
        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<Session> listSessions(SessionStatus status) {
        return status == null ? sessionRepository.findAllByOrderByStartTimeAsc() : sessionRepository.findByStatusOrderByStartTimeAsc(status);
    }

    @Transactional(readOnly = true)
    public Session findSession(UUID sessionUuid) {
        return sessionRepository.findById(sessionUuid).orElseThrow(() -> new SessionNotFoundException("Session not found"));
    }

    @Transactional(readOnly = true)
    public List<Enrollment> listSessionEnrollments(UUID sessionUuid) {
        Session session = findSession(sessionUuid);

        return enrollmentRepository.findBySession(session).stream().sorted(SESSION_ENROLLMENT_ORDER).toList();
    }

    @Transactional(readOnly = true)
    public List<Enrollment> listUserEnrollments(UUID userUuid) {
        User student = userRepository.findById(userUuid).orElseThrow(() -> new UserNotFoundException("User not found"));

        return enrollmentRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    private static int statusRank(Enrollment enrollment) {
        return switch (enrollment.getStatus()) {
            case CONFIRMED -> 0;
            case WAITLISTED -> 1;
            default -> 2;
        };
    }

    private static int queueRank(Enrollment enrollment) {
        return enrollment.getStatus() == EnrollmentStatus.WAITLISTED && enrollment.getQueuePosition() != null ? enrollment.getQueuePosition() : 0;
    }

    private void cancelActiveEnrollments(Session session, String reason) {
        List<Enrollment> active = enrollmentRepository.findBySessionAndStatusIn(session, List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED));
        if (active.isEmpty()) {
            return;
        }

        for (Enrollment enrollment : active) {
            enrollment.setStatus(EnrollmentStatus.CANCELLED);
            enrollment.setQueuePosition(null);
        }
        enrollmentRepository.saveAll(active);

        String message = "Your " + session.getBond().getSubject().getName() + " session on "
                + NOTIFICATION_TIME.format(session.getStartTime().withOffsetSameInstant(ZoneOffset.UTC)) + " was cancelled" + reason + ".";
        notificationRepository.saveAll(active.stream()
                .map(enrollment -> new Notification(NotificationEventType.SESSION_CANCELLED, message, enrollment.getStudent()))
                .toList());
    }

    private boolean isRequesterOrEvaluator(TutoringBond tutoringBond, UUID userId) {
        return (tutoringBond.getRequester().getId().equals(userId)) || (tutoringBond.getEvaluator() != null && tutoringBond.getEvaluator().getId().equals(userId));
    }

    private boolean isEvaluator(TutoringBond tutoringBond, UUID userId) {
        return (tutoringBond.getEvaluator() != null && tutoringBond.getEvaluator().getId().equals(userId));
    }
}
