package com.joaoguilherme.tutoringmanagementsystem.service;

import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidSubjectStatusException;
import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidTutoringBondStatusException;
import com.joaoguilherme.tutoringmanagementsystem.exception.SubjectNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.TutoringBondAlreadyExistsException;
import com.joaoguilherme.tutoringmanagementsystem.exception.TutoringBondNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.UserNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.UserWithoutPermissionException;
import com.joaoguilherme.tutoringmanagementsystem.model.Subject;
import com.joaoguilherme.tutoringmanagementsystem.model.TutoringBond;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.BondStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SubjectStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;
import com.joaoguilherme.tutoringmanagementsystem.repository.SubjectRepository;
import com.joaoguilherme.tutoringmanagementsystem.repository.TutoringBondRepository;
import com.joaoguilherme.tutoringmanagementsystem.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TutoringBondServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TutoringBondRepository tutoringBondRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private TutoringBondService tutoringBondService;

    private User newAdmin() {
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);
        return admin;
    }

    private User newRegularUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private TutoringBond newBond(UUID tutoringBondUuid, BondStatus status) {
        TutoringBond tutoringBond = new TutoringBond(newRegularUser(), new Subject());
        tutoringBond.setId(tutoringBondUuid);
        tutoringBond.setStatus(status);
        return tutoringBond;
    }

    @Test
    void shouldApproveTutoringBondWhenStatusIsPending() {
        UUID tutoringBondUuid = UUID.randomUUID();
        TutoringBond tutoringBond = newBond(tutoringBondUuid, BondStatus.PENDING);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));
        when(tutoringBondRepository.save(any(TutoringBond.class))).thenReturn(tutoringBond);

        TutoringBond tutoringBondTest = tutoringBondService.approveTutoringBond(tutoringBondUuid, admin.getId());

        Assertions.assertEquals(BondStatus.APPROVED, tutoringBondTest.getStatus());

        Assertions.assertEquals(admin, tutoringBondTest.getEvaluator());
    }

    @Test
    void shouldThrowTutoringBondNotFoundExceptionWhenBondDoesNotExist() {
        UUID tutoringBondUuid = UUID.randomUUID();
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(TutoringBondNotFoundException.class, () -> tutoringBondService.approveTutoringBond(tutoringBondUuid, admin.getId()));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenEvaluatorDoesNotExist() {
        UUID evaluatorUuid = UUID.randomUUID();

        when(userRepository.findById(evaluatorUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> tutoringBondService.approveTutoringBond(UUID.randomUUID(), evaluatorUuid));

        verifyNoInteractions(tutoringBondRepository);
    }

    @Test
    void shouldThrowInvalidTutoringBondStatusExceptionWhenStatusIsNotPending() {
        UUID tutoringBondUuid = UUID.randomUUID();
        TutoringBond tutoringBond = newBond(tutoringBondUuid, BondStatus.APPROVED);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));

        Assertions.assertThrows(InvalidTutoringBondStatusException.class, () -> tutoringBondService.approveTutoringBond(tutoringBondUuid, admin.getId()));
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenRegularUserApprovesBond() {
        User user = newRegularUser();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> tutoringBondService.approveTutoringBond(UUID.randomUUID(), user.getId()));

        verifyNoInteractions(tutoringBondRepository);
    }

    @Test
    void shouldRejectTutoringBondWhenStatusIsPending() {
        UUID tutoringBondUuid = UUID.randomUUID();
        TutoringBond tutoringBond = newBond(tutoringBondUuid, BondStatus.PENDING);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));
        when(tutoringBondRepository.save(any(TutoringBond.class))).thenReturn(tutoringBond);

        TutoringBond tutoringBondTest = tutoringBondService.rejectTutoringBond(tutoringBondUuid, admin.getId());

        Assertions.assertEquals(BondStatus.REJECTED, tutoringBondTest.getStatus());

        Assertions.assertEquals(admin, tutoringBondTest.getEvaluator());
    }

    @Test
    void shouldThrowTutoringBondNotFoundExceptionWhenRejectingNonexistentBond() {
        UUID tutoringBondUuid = UUID.randomUUID();
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(TutoringBondNotFoundException.class, () -> tutoringBondService.rejectTutoringBond(tutoringBondUuid, admin.getId()));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenRejectingWithNonexistentEvaluator() {
        UUID evaluatorUuid = UUID.randomUUID();

        when(userRepository.findById(evaluatorUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> tutoringBondService.rejectTutoringBond(UUID.randomUUID(), evaluatorUuid));

        verifyNoInteractions(tutoringBondRepository);
    }

    @Test
    void shouldThrowInvalidTutoringBondStatusExceptionWhenRejectingNonPendingBond() {
        UUID tutoringBondUuid = UUID.randomUUID();
        TutoringBond tutoringBond = newBond(tutoringBondUuid, BondStatus.APPROVED);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));

        Assertions.assertThrows(InvalidTutoringBondStatusException.class, () -> tutoringBondService.rejectTutoringBond(tutoringBondUuid, admin.getId()));
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenRegularUserRejectsBond() {
        User user = newRegularUser();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> tutoringBondService.rejectTutoringBond(UUID.randomUUID(), user.getId()));

        verifyNoInteractions(tutoringBondRepository);
    }

    @Test
    void shouldRevokeTutoringBondWhenStatusIsApproved() {
        UUID tutoringBondUuid = UUID.randomUUID();
        TutoringBond tutoringBond = newBond(tutoringBondUuid, BondStatus.APPROVED);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));
        when(tutoringBondRepository.save(any(TutoringBond.class))).thenReturn(tutoringBond);

        TutoringBond tutoringBondTest = tutoringBondService.revokeTutoringBond(tutoringBondUuid, admin.getId());

        Assertions.assertEquals(BondStatus.REVOKED, tutoringBondTest.getStatus());

        Assertions.assertEquals(admin, tutoringBondTest.getEvaluator());
    }

    @Test
    void shouldThrowTutoringBondNotFoundExceptionWhenRevokingNonexistentBond() {
        UUID tutoringBondUuid = UUID.randomUUID();
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(TutoringBondNotFoundException.class, () -> tutoringBondService.revokeTutoringBond(tutoringBondUuid, admin.getId()));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenRevokingWithNonexistentEvaluator() {
        UUID evaluatorUuid = UUID.randomUUID();

        when(userRepository.findById(evaluatorUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> tutoringBondService.revokeTutoringBond(UUID.randomUUID(), evaluatorUuid));

        verifyNoInteractions(tutoringBondRepository);
    }

    @Test
    void shouldThrowInvalidTutoringBondStatusExceptionWhenRevokingNonApprovedBond() {
        UUID tutoringBondUuid = UUID.randomUUID();
        TutoringBond tutoringBond = newBond(tutoringBondUuid, BondStatus.REVOKED);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));

        Assertions.assertThrows(InvalidTutoringBondStatusException.class, () -> tutoringBondService.revokeTutoringBond(tutoringBondUuid, admin.getId()));
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenRegularUserRevokesBond() {
        User user = newRegularUser();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> tutoringBondService.revokeTutoringBond(UUID.randomUUID(), user.getId()));

        verifyNoInteractions(tutoringBondRepository);
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldCancelFutureSessionsWhenTutoringBondIsRevoked() {
        UUID tutoringBondUuid = UUID.randomUUID();
        TutoringBond tutoringBond = newBond(tutoringBondUuid, BondStatus.APPROVED);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));
        when(tutoringBondRepository.save(any(TutoringBond.class))).thenReturn(tutoringBond);

        tutoringBondService.revokeTutoringBond(tutoringBondUuid, admin.getId());

        verify(sessionService).cancelFutureSessions(tutoringBond);
    }

    @Test
    void shouldNotCancelSessionsWhenTutoringBondIsNotApproved() {
        UUID tutoringBondUuid = UUID.randomUUID();
        TutoringBond tutoringBond = newBond(tutoringBondUuid, BondStatus.PENDING);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(tutoringBondUuid)).thenReturn(Optional.of(tutoringBond));

        Assertions.assertThrows(InvalidTutoringBondStatusException.class, () -> tutoringBondService.revokeTutoringBond(tutoringBondUuid, admin.getId()));

        verifyNoInteractions(sessionService);
    }

    private Subject subjectWithStatus(SubjectStatus status) {
        Subject subject = new Subject(new User(), "Calculus I");
        subject.setId(UUID.randomUUID());
        subject.setStatus(status);
        return subject;
    }

    @Test
    void shouldRequestTutoringBondWhenSubjectIsApprovedAndNoActiveBondExists() {
        Subject subject = subjectWithStatus(SubjectStatus.APPROVED);

        User requester = new User();
        requester.setId(UUID.randomUUID());

        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(tutoringBondRepository.findByRequesterAndSubjectAndStatusIn(requester, subject, List.of(BondStatus.PENDING, BondStatus.APPROVED))).thenReturn(List.of());
        when(tutoringBondRepository.save(any(TutoringBond.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TutoringBond tutoringBond = tutoringBondService.requestTutoringBond(subject.getId(), requester.getId());

        Assertions.assertEquals(BondStatus.PENDING, tutoringBond.getStatus());
        Assertions.assertEquals(requester, tutoringBond.getRequester());
        Assertions.assertEquals(subject, tutoringBond.getSubject());
        Assertions.assertNull(tutoringBond.getEvaluator());
    }

    @Test
    void shouldThrowSubjectNotFoundExceptionWhenRequestingBondForUnknownSubject() {
        UUID subjectUuid = UUID.randomUUID();

        when(subjectRepository.findById(subjectUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(SubjectNotFoundException.class, () -> tutoringBondService.requestTutoringBond(subjectUuid, UUID.randomUUID()));

        verifyNoInteractions(userRepository, tutoringBondRepository);
    }

    @Test
    void shouldThrowInvalidSubjectStatusExceptionWhenSubjectIsPending() {
        Subject subject = subjectWithStatus(SubjectStatus.PENDING);

        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));

        Assertions.assertThrows(InvalidSubjectStatusException.class, () -> tutoringBondService.requestTutoringBond(subject.getId(), UUID.randomUUID()));

        verifyNoInteractions(userRepository, tutoringBondRepository);
    }

    @Test
    void shouldThrowInvalidSubjectStatusExceptionWhenSubjectIsRejected() {
        Subject subject = subjectWithStatus(SubjectStatus.REJECTED);

        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));

        Assertions.assertThrows(InvalidSubjectStatusException.class, () -> tutoringBondService.requestTutoringBond(subject.getId(), UUID.randomUUID()));

        verifyNoInteractions(userRepository, tutoringBondRepository);
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenRequesterDoesNotExist() {
        Subject subject = subjectWithStatus(SubjectStatus.APPROVED);
        UUID requesterUuid = UUID.randomUUID();

        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(userRepository.findById(requesterUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> tutoringBondService.requestTutoringBond(subject.getId(), requesterUuid));

        verifyNoInteractions(tutoringBondRepository);
    }

    @Test
    void shouldThrowTutoringBondAlreadyExistsExceptionWhenActiveBondExists() {
        Subject subject = subjectWithStatus(SubjectStatus.APPROVED);

        User requester = new User();
        requester.setId(UUID.randomUUID());

        TutoringBond existingBond = new TutoringBond(requester, subject);

        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(tutoringBondRepository.findByRequesterAndSubjectAndStatusIn(requester, subject, List.of(BondStatus.PENDING, BondStatus.APPROVED))).thenReturn(List.of(existingBond));

        Assertions.assertThrows(TutoringBondAlreadyExistsException.class, () -> tutoringBondService.requestTutoringBond(subject.getId(), requester.getId()));

        verify(tutoringBondRepository, never()).save(any(TutoringBond.class));
    }

    @Test
    void shouldListEveryBondWhenUserIsAdmin() {
        User admin = newAdmin();
        List<TutoringBond> bonds = List.of(newBond(UUID.randomUUID(), BondStatus.PENDING), newBond(UUID.randomUUID(), BondStatus.APPROVED));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findAllByOrderByCreatedAtDesc()).thenReturn(bonds);

        Assertions.assertEquals(bonds, tutoringBondService.listTutoringBonds(admin.getId(), null));
    }

    @Test
    void shouldListEveryBondWithStatusWhenAdminFiltersByStatus() {
        User admin = newAdmin();
        List<TutoringBond> pending = List.of(newBond(UUID.randomUUID(), BondStatus.PENDING));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findByStatusOrderByCreatedAtDesc(BondStatus.PENDING)).thenReturn(pending);

        Assertions.assertEquals(pending, tutoringBondService.listTutoringBonds(admin.getId(), BondStatus.PENDING));
    }

    @Test
    void shouldListOnlyOwnBondsWhenUserIsRegular() {
        User user = newRegularUser();
        List<TutoringBond> own = List.of(newBond(UUID.randomUUID(), BondStatus.APPROVED));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tutoringBondRepository.findByRequesterOrderByCreatedAtDesc(user)).thenReturn(own);

        Assertions.assertEquals(own, tutoringBondService.listTutoringBonds(user.getId(), null));
        verify(tutoringBondRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void shouldListOwnBondsWithStatusWhenRegularUserFiltersByStatus() {
        User user = newRegularUser();
        List<TutoringBond> own = List.of(newBond(UUID.randomUUID(), BondStatus.REVOKED));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tutoringBondRepository.findByRequesterAndStatusOrderByCreatedAtDesc(user, BondStatus.REVOKED)).thenReturn(own);

        Assertions.assertEquals(own, tutoringBondService.listTutoringBonds(user.getId(), BondStatus.REVOKED));
        verify(tutoringBondRepository, never()).findByStatusOrderByCreatedAtDesc(any());
    }

    @Test
    void shouldThrowUserNotFoundWhenListingBondsForMissingUser() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> tutoringBondService.listTutoringBonds(missing, null));
        verifyNoInteractions(tutoringBondRepository);
    }

    @Test
    void shouldReturnAnyBondWhenAdminFindsIt() {
        User admin = newAdmin();
        TutoringBond bond = newBond(UUID.randomUUID(), BondStatus.PENDING);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(bond.getId())).thenReturn(Optional.of(bond));

        Assertions.assertSame(bond, tutoringBondService.findTutoringBond(bond.getId(), admin.getId()));
    }

    @Test
    void shouldReturnBondWhenRequesterFindsIt() {
        TutoringBond bond = newBond(UUID.randomUUID(), BondStatus.APPROVED);
        User requester = bond.getRequester();
        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(tutoringBondRepository.findById(bond.getId())).thenReturn(Optional.of(bond));

        Assertions.assertSame(bond, tutoringBondService.findTutoringBond(bond.getId(), requester.getId()));
    }

    @Test
    void shouldThrowUserWithoutPermissionWhenRegularUserFindsAnotherUsersBond() {
        User other = newRegularUser();
        TutoringBond bond = newBond(UUID.randomUUID(), BondStatus.APPROVED);
        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(tutoringBondRepository.findById(bond.getId())).thenReturn(Optional.of(bond));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> tutoringBondService.findTutoringBond(bond.getId(), other.getId()));
    }

    @Test
    void shouldThrowTutoringBondNotFoundWhenFindingMissingBond() {
        User admin = newAdmin();
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(tutoringBondRepository.findById(missing)).thenReturn(Optional.empty());

        Assertions.assertThrows(TutoringBondNotFoundException.class, () -> tutoringBondService.findTutoringBond(missing, admin.getId()));
    }
}
