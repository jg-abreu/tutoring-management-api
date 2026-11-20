package com.joaoguilherme.tutoringmanagementsystem.service;

import com.joaoguilherme.tutoringmanagementsystem.exception.EnrollmentAlreadyExistsException;
import com.joaoguilherme.tutoringmanagementsystem.exception.EnrollmentNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidEnrollmentStatusException;
import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidSessionStatusException;
import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidTimeException;
import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidTutoringBondStatusException;
import com.joaoguilherme.tutoringmanagementsystem.exception.SessionFullException;
import com.joaoguilherme.tutoringmanagementsystem.exception.SessionNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.TutorCannotEnrollException;
import com.joaoguilherme.tutoringmanagementsystem.exception.TutoringBondNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.UserNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.UserWithoutPermissionException;
import com.joaoguilherme.tutoringmanagementsystem.model.Enrollment;
import com.joaoguilherme.tutoringmanagementsystem.model.Notification;
import com.joaoguilherme.tutoringmanagementsystem.model.Session;
import com.joaoguilherme.tutoringmanagementsystem.model.Subject;
import com.joaoguilherme.tutoringmanagementsystem.model.TutoringBond;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.BondStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.EnrollmentStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.NotificationEventType;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SessionStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;
import com.joaoguilherme.tutoringmanagementsystem.repository.EnrollmentRepository;
import com.joaoguilherme.tutoringmanagementsystem.repository.NotificationRepository;
import com.joaoguilherme.tutoringmanagementsystem.repository.SessionRepository;
import com.joaoguilherme.tutoringmanagementsystem.repository.TutoringBondRepository;
import com.joaoguilherme.tutoringmanagementsystem.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.CREATED_AT;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.bond;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.enrollment;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.session;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.subject;
import static com.joaoguilherme.tutoringmanagementsystem.dto.ResponseFixtures.user;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SessionServiceTest {

    @Mock
    private TutoringBondRepository tutoringBondRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void shouldCreateSessionWhenUserIsRequester() {
        UUID tutoringBondUuid = UUID.randomUUID();

        User requester = new User();
        requester.setId(UUID.randomUUID());

        TutoringBond tutoringBond = new TutoringBond(requester, new Subject());
        tutoringBond.setId(tutoringBondUuid);
        tutoringBond.setStatus(BondStatus.APPROVED);

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endTime = startTime.plusHours(2);

        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));
        when(sessionRepository.save(any(Session.class))).then(returnsFirstArg());

        Session sessionTest = sessionService.createSession(tutoringBondUuid, requester.getId(), startTime, endTime, 5, true);

        Assertions.assertEquals(startTime, sessionTest.getStartTime());
        Assertions.assertEquals(endTime, sessionTest.getEndTime());
        Assertions.assertEquals(5, sessionTest.getSpots());
        Assertions.assertTrue(sessionTest.isAllowsWaitlist());
        Assertions.assertEquals(tutoringBond, sessionTest.getBond());
        Assertions.assertEquals(SessionStatus.ACTIVE, sessionTest.getStatus());

        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void shouldCreateSessionWhenUserIsEvaluator() {
        UUID tutoringBondUuid = UUID.randomUUID();

        User requester = new User();
        requester.setId(UUID.randomUUID());

        User evaluator = new User();
        evaluator.setId(UUID.randomUUID());

        TutoringBond tutoringBond = new TutoringBond(requester, new Subject());
        tutoringBond.setId(tutoringBondUuid);
        tutoringBond.setStatus(BondStatus.APPROVED);
        tutoringBond.setEvaluator(evaluator);

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endTime = startTime.plusHours(2);

        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));
        when(sessionRepository.save(any(Session.class))).then(returnsFirstArg());

        Session sessionTest = sessionService.createSession(tutoringBondUuid, evaluator.getId(), startTime, endTime, 5, false);

        Assertions.assertEquals(tutoringBond, sessionTest.getBond());

        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void shouldThrowInvalidTimeExceptionWhenEndTimeIsBeforeStartTime() {
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endTime = startTime.minusHours(1);

        Assertions.assertThrows(InvalidTimeException.class, () -> sessionService.createSession(UUID.randomUUID(), UUID.randomUUID(), startTime, endTime, 5, false));

        verifyNoInteractions(tutoringBondRepository, sessionRepository);
    }

    @Test
    void shouldThrowInvalidTimeExceptionWhenEndTimeEqualsStartTime() {
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);

        Assertions.assertThrows(InvalidTimeException.class, () -> sessionService.createSession(UUID.randomUUID(), UUID.randomUUID(), startTime, startTime, 5, false));

        verifyNoInteractions(tutoringBondRepository, sessionRepository);
    }

    @Test
    void shouldThrowTutoringBondNotFoundExceptionWhenBondDoesNotExist() {
        UUID tutoringBondUuid = UUID.randomUUID();

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endTime = startTime.plusHours(2);

        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(TutoringBondNotFoundException.class, () -> sessionService.createSession(tutoringBondUuid, UUID.randomUUID(), startTime, endTime, 5, false));

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenUserIsNotRequesterNorEvaluator() {
        UUID tutoringBondUuid = UUID.randomUUID();

        User requester = new User();
        requester.setId(UUID.randomUUID());

        User evaluator = new User();
        evaluator.setId(UUID.randomUUID());

        TutoringBond tutoringBond = new TutoringBond(requester, new Subject());
        tutoringBond.setId(tutoringBondUuid);
        tutoringBond.setStatus(BondStatus.APPROVED);
        tutoringBond.setEvaluator(evaluator);

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endTime = startTime.plusHours(2);

        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> sessionService.createSession(tutoringBondUuid, UUID.randomUUID(), startTime, endTime, 5, false));

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenBondIsPendingAndUserIsNotRequester() {
        UUID tutoringBondUuid = UUID.randomUUID();

        User requester = new User();
        requester.setId(UUID.randomUUID());

        TutoringBond tutoringBond = new TutoringBond(requester, new Subject());
        tutoringBond.setId(tutoringBondUuid);

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endTime = startTime.plusHours(2);

        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> sessionService.createSession(tutoringBondUuid, UUID.randomUUID(), startTime, endTime, 5, false));

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldThrowInvalidTutoringBondStatusExceptionWhenBondIsNotApproved() {
        UUID tutoringBondUuid = UUID.randomUUID();

        User requester = new User();
        requester.setId(UUID.randomUUID());

        TutoringBond tutoringBond = new TutoringBond(requester, new Subject());
        tutoringBond.setId(tutoringBondUuid);

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        OffsetDateTime endTime = startTime.plusHours(2);

        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));

        Assertions.assertThrows(InvalidTutoringBondStatusException.class, () -> sessionService.createSession(tutoringBondUuid, requester.getId(), startTime, endTime, 5, false));

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldCancelSessionWhenUserIsEvaluatorAndSessionIsActive() {
        UUID sessionUuid = UUID.randomUUID();

        User requester = new User();
        requester.setId(UUID.randomUUID());

        User evaluator = new User();
        evaluator.setId(UUID.randomUUID());

        TutoringBond tutoringBond = new TutoringBond(requester, new Subject());
        tutoringBond.setStatus(BondStatus.APPROVED);
        tutoringBond.setEvaluator(evaluator);

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 5, false, tutoringBond);
        session.setId(sessionUuid);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).then(returnsFirstArg());

        Session sessionTest = sessionService.cancelSession(sessionUuid, evaluator.getId());

        Assertions.assertEquals(SessionStatus.CANCELLED, sessionTest.getStatus());

        verify(sessionRepository).save(session);
    }

    @Test
    void shouldThrowSessionNotFoundExceptionWhenSessionDoesNotExist() {
        UUID sessionUuid = UUID.randomUUID();

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(SessionNotFoundException.class, () -> sessionService.cancelSession(sessionUuid, UUID.randomUUID()));

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenUserIsRequester() {
        UUID sessionUuid = UUID.randomUUID();

        User requester = new User();
        requester.setId(UUID.randomUUID());

        User evaluator = new User();
        evaluator.setId(UUID.randomUUID());

        TutoringBond tutoringBond = new TutoringBond(requester, new Subject());
        tutoringBond.setStatus(BondStatus.APPROVED);
        tutoringBond.setEvaluator(evaluator);

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 5, false, tutoringBond);
        session.setId(sessionUuid);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> sessionService.cancelSession(sessionUuid, requester.getId()));

        Assertions.assertEquals(SessionStatus.ACTIVE, session.getStatus());

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenBondHasNoEvaluator() {
        UUID sessionUuid = UUID.randomUUID();

        User requester = new User();
        requester.setId(UUID.randomUUID());

        TutoringBond tutoringBond = new TutoringBond(requester, new Subject());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 5, false, tutoringBond);
        session.setId(sessionUuid);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> sessionService.cancelSession(sessionUuid, UUID.randomUUID()));

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldThrowInvalidSessionStatusExceptionWhenSessionIsAlreadyCancelled() {
        UUID sessionUuid = UUID.randomUUID();

        User requester = new User();
        requester.setId(UUID.randomUUID());

        User evaluator = new User();
        evaluator.setId(UUID.randomUUID());

        TutoringBond tutoringBond = new TutoringBond(requester, new Subject());
        tutoringBond.setStatus(BondStatus.APPROVED);
        tutoringBond.setEvaluator(evaluator);

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 5, false, tutoringBond);
        session.setId(sessionUuid);
        session.setStatus(SessionStatus.CANCELLED);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));

        Assertions.assertThrows(InvalidSessionStatusException.class, () -> sessionService.cancelSession(sessionUuid, evaluator.getId()));

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void shouldCancelFutureActiveSessionsWhenBondHasSessions() {
        TutoringBond tutoringBond = new TutoringBond(new User(), new Subject());
        tutoringBond.setStatus(BondStatus.APPROVED);

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session firstSession = new Session(startTime, startTime.plusHours(2), 5, false, tutoringBond);
        Session secondSession = new Session(startTime.plusDays(7), startTime.plusDays(7).plusHours(2), 5, false, tutoringBond);
        List<Session> futureSessions = List.of(firstSession, secondSession);

        when(sessionRepository.findByBondAndStatusAndStartTimeAfter(eq(tutoringBond), eq(SessionStatus.ACTIVE), any(OffsetDateTime.class))).thenReturn(futureSessions);
        when(sessionRepository.saveAll(futureSessions)).thenReturn(futureSessions);

        List<Session> cancelledSessions = sessionService.cancelFutureSessions(tutoringBond);

        Assertions.assertEquals(2, cancelledSessions.size());
        Assertions.assertEquals(SessionStatus.CANCELLED, firstSession.getStatus());
        Assertions.assertEquals(SessionStatus.CANCELLED, secondSession.getStatus());

        verify(sessionRepository).saveAll(futureSessions);
    }

    @Test
    void shouldQueryOnlyActiveSessionsWhenCancellingFutureSessions() {
        TutoringBond tutoringBond = new TutoringBond(new User(), new Subject());

        OffsetDateTime before = OffsetDateTime.now();

        sessionService.cancelFutureSessions(tutoringBond);

        OffsetDateTime after = OffsetDateTime.now();

        ArgumentCaptor<OffsetDateTime> referenceTime = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(sessionRepository).findByBondAndStatusAndStartTimeAfter(eq(tutoringBond), eq(SessionStatus.ACTIVE), referenceTime.capture());

        Assertions.assertFalse(referenceTime.getValue().isBefore(before));
        Assertions.assertFalse(referenceTime.getValue().isAfter(after));
    }

    @Test
    void shouldReturnEmptyListWhenBondHasNoFutureSessions() {
        TutoringBond tutoringBond = new TutoringBond(new User(), new Subject());

        when(sessionRepository.findByBondAndStatusAndStartTimeAfter(eq(tutoringBond), eq(SessionStatus.ACTIVE), any(OffsetDateTime.class))).thenReturn(List.of());
        when(sessionRepository.saveAll(List.<Session>of())).thenReturn(List.of());

        List<Session> cancelledSessions = sessionService.cancelFutureSessions(tutoringBond);

        Assertions.assertTrue(cancelledSessions.isEmpty());
    }

    @Test
    void shouldConfirmEnrollmentWhenThereAreAvailableSpots() {
        UUID sessionUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 5, false, new TutoringBond(new User(), new Subject()));
        session.setId(sessionUuid);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsBySessionAndStudentAndStatusIn(session, student, List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED))).thenReturn(false);
        when(enrollmentRepository.countBySessionAndStatus(session, EnrollmentStatus.CONFIRMED)).thenReturn(4L);
        when(enrollmentRepository.save(any(Enrollment.class))).then(returnsFirstArg());

        Enrollment enrollmentTest = sessionService.enroll(sessionUuid, student.getId());

        Assertions.assertEquals(EnrollmentStatus.CONFIRMED, enrollmentTest.getStatus());
        Assertions.assertNull(enrollmentTest.getQueuePosition());
        Assertions.assertEquals(session, enrollmentTest.getSession());
        Assertions.assertEquals(student, enrollmentTest.getStudent());

        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void shouldAddToWaitlistWhenSessionIsFullAndAllowsWaitlist() {
        UUID sessionUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 2, true, new TutoringBond(new User(), new Subject()));
        session.setId(sessionUuid);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsBySessionAndStudentAndStatusIn(session, student, List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED))).thenReturn(false);
        when(enrollmentRepository.countBySessionAndStatus(session, EnrollmentStatus.CONFIRMED)).thenReturn(2L);
        when(enrollmentRepository.countBySessionAndStatus(session, EnrollmentStatus.WAITLISTED)).thenReturn(2L);
        when(enrollmentRepository.save(any(Enrollment.class))).then(returnsFirstArg());

        Enrollment enrollmentTest = sessionService.enroll(sessionUuid, student.getId());

        Assertions.assertEquals(EnrollmentStatus.WAITLISTED, enrollmentTest.getStatus());
        Assertions.assertEquals(3, enrollmentTest.getQueuePosition());
        Assertions.assertEquals(session, enrollmentTest.getSession());
        Assertions.assertEquals(student, enrollmentTest.getStudent());

        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void shouldThrowSessionFullExceptionWhenSessionIsFullAndDoesNotAllowWaitlist() {
        UUID sessionUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 2, false, new TutoringBond(new User(), new Subject()));
        session.setId(sessionUuid);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsBySessionAndStudentAndStatusIn(session, student, List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED))).thenReturn(false);
        when(enrollmentRepository.countBySessionAndStatus(session, EnrollmentStatus.CONFIRMED)).thenReturn(2L);

        Assertions.assertThrows(SessionFullException.class, () -> sessionService.enroll(sessionUuid, student.getId()));

        verify(enrollmentRepository, never()).countBySessionAndStatus(session, EnrollmentStatus.WAITLISTED);
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void shouldThrowSessionNotFoundExceptionWhenEnrollingInNonexistentSession() {
        UUID sessionUuid = UUID.randomUUID();

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(SessionNotFoundException.class, () -> sessionService.enroll(sessionUuid, UUID.randomUUID()));

        verifyNoInteractions(userRepository, enrollmentRepository);
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenStudentDoesNotExist() {
        UUID sessionUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 5, false, new TutoringBond(new User(), new Subject()));
        session.setId(sessionUuid);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(userRepository.findById(studentUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> sessionService.enroll(sessionUuid, studentUuid));

        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void shouldThrowInvalidSessionStatusExceptionWhenEnrollingInCancelledSession() {
        UUID sessionUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 5, false, new TutoringBond(new User(), new Subject()));
        session.setId(sessionUuid);
        session.setStatus(SessionStatus.CANCELLED);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        Assertions.assertThrows(InvalidSessionStatusException.class, () -> sessionService.enroll(sessionUuid, student.getId()));

        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void shouldThrowEnrollmentAlreadyExistsExceptionWhenStudentIsAlreadyEnrolled() {
        UUID sessionUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 5, false, new TutoringBond(new User(), new Subject()));
        session.setId(sessionUuid);

        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(session));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsBySessionAndStudentAndStatusIn(session, student, List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED))).thenReturn(true);

        Assertions.assertThrows(EnrollmentAlreadyExistsException.class, () -> sessionService.enroll(sessionUuid, student.getId()));

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void shouldPromoteFirstWaitlistedWhenConfirmedEnrollmentIsCancelled() {
        UUID enrollmentUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        User promotedStudent = new User();
        promotedStudent.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 2, true, new TutoringBond(new User(), new Subject()));

        Enrollment enrollment = new Enrollment(EnrollmentStatus.CONFIRMED, session, student);
        enrollment.setId(enrollmentUuid);

        Enrollment firstWaiting = new Enrollment(EnrollmentStatus.WAITLISTED, session, promotedStudent);
        firstWaiting.setQueuePosition(1);
        Enrollment secondWaiting = new Enrollment(EnrollmentStatus.WAITLISTED, session, new User());
        secondWaiting.setQueuePosition(2);
        Enrollment thirdWaiting = new Enrollment(EnrollmentStatus.WAITLISTED, session, new User());
        thirdWaiting.setQueuePosition(3);
        List<Enrollment> waitlist = List.of(firstWaiting, secondWaiting, thirdWaiting);

        when(enrollmentRepository.findById(enrollmentUuid)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.findBySessionAndStatusOrderByQueuePositionAsc(session, EnrollmentStatus.WAITLISTED)).thenReturn(waitlist);
        when(enrollmentRepository.save(any(Enrollment.class))).then(returnsFirstArg());

        Enrollment enrollmentTest = sessionService.cancelEnrollment(enrollmentUuid, student.getId());

        Assertions.assertEquals(EnrollmentStatus.CANCELLED, enrollmentTest.getStatus());
        Assertions.assertNull(enrollmentTest.getQueuePosition());

        Assertions.assertEquals(EnrollmentStatus.CONFIRMED, firstWaiting.getStatus());
        Assertions.assertNull(firstWaiting.getQueuePosition());
        Assertions.assertEquals(1, secondWaiting.getQueuePosition());
        Assertions.assertEquals(2, thirdWaiting.getQueuePosition());

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notification.capture());

        Assertions.assertEquals(NotificationEventType.SPOT_PROMOTED, notification.getValue().getEventType());
        Assertions.assertEquals(promotedStudent, notification.getValue().getReceiver());

        verify(enrollmentRepository).saveAll(waitlist);
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    void shouldCancelConfirmedEnrollmentWithoutPromotionWhenWaitlistIsEmpty() {
        UUID enrollmentUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 2, true, new TutoringBond(new User(), new Subject()));

        Enrollment enrollment = new Enrollment(EnrollmentStatus.CONFIRMED, session, student);
        enrollment.setId(enrollmentUuid);

        when(enrollmentRepository.findById(enrollmentUuid)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.findBySessionAndStatusOrderByQueuePositionAsc(session, EnrollmentStatus.WAITLISTED)).thenReturn(List.of());
        when(enrollmentRepository.save(any(Enrollment.class))).then(returnsFirstArg());

        Enrollment enrollmentTest = sessionService.cancelEnrollment(enrollmentUuid, student.getId());

        Assertions.assertEquals(EnrollmentStatus.CANCELLED, enrollmentTest.getStatus());

        verifyNoInteractions(notificationRepository);
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    void shouldReorderWaitlistWhenWaitlistedEnrollmentIsCancelled() {
        UUID enrollmentUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 2, true, new TutoringBond(new User(), new Subject()));

        Enrollment enrollment = new Enrollment(EnrollmentStatus.WAITLISTED, session, student);
        enrollment.setId(enrollmentUuid);
        enrollment.setQueuePosition(1);

        Enrollment secondWaiting = new Enrollment(EnrollmentStatus.WAITLISTED, session, new User());
        secondWaiting.setQueuePosition(2);
        Enrollment thirdWaiting = new Enrollment(EnrollmentStatus.WAITLISTED, session, new User());
        thirdWaiting.setQueuePosition(3);
        List<Enrollment> waitlist = List.of(secondWaiting, thirdWaiting);

        when(enrollmentRepository.findById(enrollmentUuid)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.findBySessionAndStatusOrderByQueuePositionAsc(session, EnrollmentStatus.WAITLISTED)).thenReturn(waitlist);
        when(enrollmentRepository.save(any(Enrollment.class))).then(returnsFirstArg());

        Enrollment enrollmentTest = sessionService.cancelEnrollment(enrollmentUuid, student.getId());

        Assertions.assertEquals(EnrollmentStatus.CANCELLED, enrollmentTest.getStatus());
        Assertions.assertNull(enrollmentTest.getQueuePosition());

        Assertions.assertEquals(EnrollmentStatus.WAITLISTED, secondWaiting.getStatus());
        Assertions.assertEquals(1, secondWaiting.getQueuePosition());
        Assertions.assertEquals(EnrollmentStatus.WAITLISTED, thirdWaiting.getStatus());
        Assertions.assertEquals(2, thirdWaiting.getQueuePosition());

        verifyNoInteractions(notificationRepository);
        verify(enrollmentRepository).saveAll(waitlist);
    }

    @Test
    void shouldThrowEnrollmentNotFoundExceptionWhenEnrollmentDoesNotExist() {
        UUID enrollmentUuid = UUID.randomUUID();

        when(enrollmentRepository.findById(enrollmentUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(EnrollmentNotFoundException.class, () -> sessionService.cancelEnrollment(enrollmentUuid, UUID.randomUUID()));

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenUserIsNotEnrollmentStudent() {
        UUID enrollmentUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 2, true, new TutoringBond(new User(), new Subject()));

        Enrollment enrollment = new Enrollment(EnrollmentStatus.CONFIRMED, session, student);
        enrollment.setId(enrollmentUuid);

        when(enrollmentRepository.findById(enrollmentUuid)).thenReturn(Optional.of(enrollment));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> sessionService.cancelEnrollment(enrollmentUuid, UUID.randomUUID()));

        Assertions.assertEquals(EnrollmentStatus.CONFIRMED, enrollment.getStatus());

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void shouldThrowInvalidEnrollmentStatusExceptionWhenEnrollmentIsAlreadyCancelled() {
        UUID enrollmentUuid = UUID.randomUUID();

        User student = new User();
        student.setId(UUID.randomUUID());

        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);
        Session session = new Session(startTime, startTime.plusHours(2), 2, true, new TutoringBond(new User(), new Subject()));

        Enrollment enrollment = new Enrollment(EnrollmentStatus.CANCELLED, session, student);
        enrollment.setId(enrollmentUuid);

        when(enrollmentRepository.findById(enrollmentUuid)).thenReturn(Optional.of(enrollment));

        Assertions.assertThrows(InvalidEnrollmentStatusException.class, () -> sessionService.cancelEnrollment(enrollmentUuid, student.getId()));

        verify(enrollmentRepository, never()).findBySessionAndStatusOrderByQueuePositionAsc(any(Session.class), any(EnrollmentStatus.class));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verifyNoInteractions(notificationRepository);
    }

    private Session readableSession() {
        User tutor = user("tutor@tutoring.dev", UserRole.USER);
        User admin = user("admin@tutoring.dev", UserRole.ADMIN);
        return session(bond(tutor, admin, subject(tutor, admin)));
    }

    private Enrollment enrollmentAt(Session session, EnrollmentStatus status, Integer queuePosition, int minutes) {
        Enrollment enrollment = enrollment(session, user("student" + minutes + "@tutoring.dev", UserRole.USER), status, queuePosition);
        enrollment.setCreatedAt(CREATED_AT.plusMinutes(minutes));
        return enrollment;
    }

    @Test
    void shouldListEverySessionByStartTimeWhenNoStatusIsGiven() {
        List<Session> sessions = List.of(readableSession(), readableSession());
        when(sessionRepository.findAllByOrderByStartTimeAsc()).thenReturn(sessions);

        Assertions.assertEquals(sessions, sessionService.listSessions(null));
    }

    @Test
    void shouldListOnlySessionsWithStatusWhenStatusIsGiven() {
        List<Session> cancelled = List.of(readableSession());
        when(sessionRepository.findByStatusOrderByStartTimeAsc(SessionStatus.CANCELLED)).thenReturn(cancelled);

        Assertions.assertEquals(cancelled, sessionService.listSessions(SessionStatus.CANCELLED));
        verify(sessionRepository, never()).findAllByOrderByStartTimeAsc();
    }

    @Test
    void shouldReturnSessionWhenFindingExistingId() {
        Session session = readableSession();
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        Assertions.assertSame(session, sessionService.findSession(session.getId()));
    }

    @Test
    void shouldThrowSessionNotFoundWhenFindingMissingId() {
        UUID missing = UUID.randomUUID();
        when(sessionRepository.findById(missing)).thenReturn(Optional.empty());

        Assertions.assertThrows(SessionNotFoundException.class, () -> sessionService.findSession(missing));
    }

    @Test
    void shouldOrderConfirmedThenWaitlistThenOthersWhenListingSessionEnrollments() {
        Session session = readableSession();
        Enrollment cancelled = enrollmentAt(session, EnrollmentStatus.CANCELLED, null, 0);
        Enrollment secondInQueue = enrollmentAt(session, EnrollmentStatus.WAITLISTED, 2, 1);
        Enrollment firstConfirmed = enrollmentAt(session, EnrollmentStatus.CONFIRMED, null, 2);
        Enrollment secondConfirmed = enrollmentAt(session, EnrollmentStatus.CONFIRMED, null, 3);
        Enrollment firstInQueue = enrollmentAt(session, EnrollmentStatus.WAITLISTED, 1, 4);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(enrollmentRepository.findBySession(session)).thenReturn(List.of(cancelled, secondInQueue, secondConfirmed, firstInQueue, firstConfirmed));

        List<Enrollment> ordered = sessionService.listSessionEnrollments(session.getId());

        Assertions.assertEquals(List.of(firstConfirmed, secondConfirmed, firstInQueue, secondInQueue, cancelled), ordered);
    }

    @Test
    void shouldThrowSessionNotFoundWhenListingEnrollmentsOfMissingSession() {
        UUID missing = UUID.randomUUID();
        when(sessionRepository.findById(missing)).thenReturn(Optional.empty());

        Assertions.assertThrows(SessionNotFoundException.class, () -> sessionService.listSessionEnrollments(missing));
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void shouldListOnlyOwnEnrollmentsWhenListingUserEnrollments() {
        User student = user("student1@tutoring.dev", UserRole.USER);
        List<Enrollment> own = List.of(enrollmentAt(readableSession(), EnrollmentStatus.CONFIRMED, null, 0));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentOrderByCreatedAtDesc(student)).thenReturn(own);

        Assertions.assertEquals(own, sessionService.listUserEnrollments(student.getId()));
    }

    @Test
    void shouldThrowUserNotFoundWhenListingEnrollmentsOfMissingUser() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> sessionService.listUserEnrollments(missing));
        verifyNoInteractions(enrollmentRepository);
    }

    private Session cancellableSession(User evaluator) {
        User tutor = user("tutor@tutoring.dev", UserRole.USER);
        Session session = session(bond(tutor, evaluator, subject(tutor, evaluator)));
        session.setStartTime(OffsetDateTime.parse("2027-03-15T14:00:00-03:00"));
        return session;
    }

    @SuppressWarnings("unchecked")
    private List<Notification> savedNotifications() {
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldCancelActiveEnrollmentsAndNotifyStudentsWhenSessionIsCancelled() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = cancellableSession(evaluator);
        Enrollment first = enrollment(session, user("student1@tutoring.dev", UserRole.USER), EnrollmentStatus.CONFIRMED, null);
        Enrollment second = enrollment(session, user("student2@tutoring.dev", UserRole.USER), EnrollmentStatus.CONFIRMED, null);
        Enrollment waiting = enrollment(session, user("student3@tutoring.dev", UserRole.USER), EnrollmentStatus.WAITLISTED, 1);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).then(returnsFirstArg());
        when(enrollmentRepository.findBySessionAndStatusIn(session, List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED)))
                .thenReturn(List.of(first, second, waiting));

        sessionService.cancelSession(session.getId(), evaluator.getId());

        for (Enrollment enrollment : List.of(first, second, waiting)) {
            Assertions.assertEquals(EnrollmentStatus.CANCELLED, enrollment.getStatus());
            Assertions.assertNull(enrollment.getQueuePosition());
        }
        verify(enrollmentRepository).saveAll(List.of(first, second, waiting));
        List<Notification> notifications = savedNotifications();
        Assertions.assertEquals(List.of(first.getStudent(), second.getStudent(), waiting.getStudent()),
                notifications.stream().map(Notification::getReceiver).toList());
        for (Notification notification : notifications) {
            Assertions.assertEquals(NotificationEventType.SESSION_CANCELLED, notification.getEventType());
            Assertions.assertEquals("Your Calculus I session on 2027-03-15 17:00 UTC was cancelled.", notification.getMessage());
        }
    }

    @Test
    void shouldLeaveAlreadyCancelledEnrollmentsAloneWhenSessionIsCancelled() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = cancellableSession(evaluator);
        Enrollment active = enrollment(session, user("student1@tutoring.dev", UserRole.USER), EnrollmentStatus.CONFIRMED, null);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).then(returnsFirstArg());
        when(enrollmentRepository.findBySessionAndStatusIn(session, List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED)))
                .thenReturn(List.of(active));

        sessionService.cancelSession(session.getId(), evaluator.getId());

        verify(enrollmentRepository).findBySessionAndStatusIn(session, List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED));
        verify(enrollmentRepository).saveAll(List.of(active));
        Assertions.assertEquals(1, savedNotifications().size());
    }

    @Test
    void shouldNotNotifyAnyoneWhenCancelledSessionHasNoActiveEnrollments() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = cancellableSession(evaluator);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).then(returnsFirstArg());
        when(enrollmentRepository.findBySessionAndStatusIn(any(), any())).thenReturn(List.of());

        Session cancelled = sessionService.cancelSession(session.getId(), evaluator.getId());

        Assertions.assertEquals(SessionStatus.CANCELLED, cancelled.getStatus());
        verify(enrollmentRepository, never()).saveAll(any());
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void shouldNotTouchEnrollmentsWhenUserCannotCancelSession() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = cancellableSession(evaluator);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> sessionService.cancelSession(session.getId(), UUID.randomUUID()));

        verifyNoInteractions(enrollmentRepository, notificationRepository);
    }

    @Test
    void shouldNotTouchEnrollmentsWhenSessionIsAlreadyCancelled() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = cancellableSession(evaluator);
        session.setStatus(SessionStatus.CANCELLED);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        Assertions.assertThrows(InvalidSessionStatusException.class, () -> sessionService.cancelSession(session.getId(), evaluator.getId()));

        verifyNoInteractions(enrollmentRepository, notificationRepository);
    }

    @Test
    void shouldCancelEnrollmentsOfEveryFutureSessionWithRevocationReasonWhenBondIsRevoked() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session first = cancellableSession(evaluator);
        Session second = session(first.getBond());
        second.setStartTime(OffsetDateTime.parse("2027-03-22T14:00:00-03:00"));
        Enrollment inFirst = enrollment(first, user("student1@tutoring.dev", UserRole.USER), EnrollmentStatus.CONFIRMED, null);
        Enrollment inSecond = enrollment(second, user("student2@tutoring.dev", UserRole.USER), EnrollmentStatus.WAITLISTED, 1);
        List<EnrollmentStatus> active = List.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.WAITLISTED);
        when(sessionRepository.findByBondAndStatusAndStartTimeAfter(eq(first.getBond()), eq(SessionStatus.ACTIVE), any(OffsetDateTime.class)))
                .thenReturn(List.of(first, second));
        when(sessionRepository.saveAll(List.of(first, second))).thenReturn(List.of(first, second));
        when(enrollmentRepository.findBySessionAndStatusIn(first, active)).thenReturn(List.of(inFirst));
        when(enrollmentRepository.findBySessionAndStatusIn(second, active)).thenReturn(List.of(inSecond));

        sessionService.cancelFutureSessions(first.getBond());

        Assertions.assertEquals(EnrollmentStatus.CANCELLED, inFirst.getStatus());
        Assertions.assertEquals(EnrollmentStatus.CANCELLED, inSecond.getStatus());
        Assertions.assertNull(inSecond.getQueuePosition());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository, times(2)).saveAll(captor.capture());
        Assertions.assertEquals(
                List.of("Your Calculus I session on 2027-03-15 17:00 UTC was cancelled because the tutor's bond was revoked.",
                        "Your Calculus I session on 2027-03-22 17:00 UTC was cancelled because the tutor's bond was revoked."),
                captor.getAllValues().stream().map(list -> list.get(0).getMessage()).toList());
    }

    @Test
    void shouldPropagateFailureWhenSavingNotificationsFails() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = cancellableSession(evaluator);
        Enrollment active = enrollment(session, user("student1@tutoring.dev", UserRole.USER), EnrollmentStatus.CONFIRMED, null);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).then(returnsFirstArg());
        when(enrollmentRepository.findBySessionAndStatusIn(any(), any())).thenReturn(List.of(active));
        when(notificationRepository.saveAll(any())).thenThrow(new IllegalStateException("database down"));

        Assertions.assertThrows(IllegalStateException.class, () -> sessionService.cancelSession(session.getId(), evaluator.getId()));
    }

    @Test
    void shouldRunCancellationsInsideTransactions() throws NoSuchMethodException {
        Assertions.assertNotNull(SessionService.class.getMethod("cancelSession", UUID.class, UUID.class).getAnnotation(Transactional.class));
        Assertions.assertNotNull(SessionService.class.getMethod("cancelFutureSessions", TutoringBond.class).getAnnotation(Transactional.class));
    }

    @Test
    void shouldThrowTutorCannotEnrollWhenTutorEnrollsInOwnSession() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = cancellableSession(evaluator);
        User tutor = session.getBond().getRequester();
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(userRepository.findById(tutor.getId())).thenReturn(Optional.of(tutor));

        TutorCannotEnrollException exception = Assertions.assertThrows(TutorCannotEnrollException.class,
                () -> sessionService.enroll(session.getId(), tutor.getId()));

        Assertions.assertEquals("Tutors cannot enroll in their own session", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void shouldEnrollEvaluatorWhenEvaluatorIsNotTheTutor() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = cancellableSession(evaluator);
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(userRepository.findById(evaluator.getId())).thenReturn(Optional.of(evaluator));
        when(enrollmentRepository.countBySessionAndStatus(session, EnrollmentStatus.CONFIRMED)).thenReturn(0L);
        when(enrollmentRepository.save(any(Enrollment.class))).then(returnsFirstArg());

        Enrollment enrollment = sessionService.enroll(session.getId(), evaluator.getId());

        Assertions.assertEquals(EnrollmentStatus.CONFIRMED, enrollment.getStatus());
        Assertions.assertEquals(evaluator, enrollment.getStudent());
    }

    @Test
    void shouldReportCancelledSessionBeforeTutorCheckWhenTutorEnrollsInCancelledSession() {
        User evaluator = user("admin@tutoring.dev", UserRole.ADMIN);
        Session session = cancellableSession(evaluator);
        session.setStatus(SessionStatus.CANCELLED);
        User tutor = session.getBond().getRequester();
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(userRepository.findById(tutor.getId())).thenReturn(Optional.of(tutor));

        Assertions.assertThrows(InvalidSessionStatusException.class, () -> sessionService.enroll(session.getId(), tutor.getId()));
    }
}
