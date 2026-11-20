package com.joaoguilherme.tutoringmanagementsystem.service;

import com.joaoguilherme.tutoringmanagementsystem.exception.InvalidSubjectStatusException;
import com.joaoguilherme.tutoringmanagementsystem.exception.SubjectAlreadyExistsException;
import com.joaoguilherme.tutoringmanagementsystem.exception.SubjectNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.UserNotFoundException;
import com.joaoguilherme.tutoringmanagementsystem.exception.UserWithoutPermissionException;
import com.joaoguilherme.tutoringmanagementsystem.model.Subject;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SubjectStatus;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;
import com.joaoguilherme.tutoringmanagementsystem.repository.SubjectRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubjectService subjectService;

    private User newUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Subject newSubject(SubjectStatus status) {
        Subject subject = new Subject(newUser(), "Calculus I");
        subject.setId(UUID.randomUUID());
        subject.setStatus(status);
        return subject;
    }

    @Test
    void shouldSuggestSubjectWhenUserExistsAndNameIsAvailable() {
        User creator = newUser();

        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(subjectRepository.existsByName("Linear Algebra")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject subject = subjectService.suggestSubject(creator.getId(), "Linear Algebra");

        Assertions.assertEquals("Linear Algebra", subject.getName());
        Assertions.assertEquals(creator, subject.getCreator());
        Assertions.assertEquals(SubjectStatus.PENDING, subject.getStatus());
        Assertions.assertNull(subject.getEvaluator());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenCreatorDoesNotExist() {
        UUID creatorUuid = UUID.randomUUID();

        when(userRepository.findById(creatorUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> subjectService.suggestSubject(creatorUuid, "Linear Algebra"));

        verifyNoInteractions(subjectRepository);
    }

    @Test
    void shouldThrowSubjectAlreadyExistsExceptionWhenNameAlreadyExists() {
        User creator = newUser();

        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(subjectRepository.existsByName("Linear Algebra")).thenReturn(true);

        Assertions.assertThrows(SubjectAlreadyExistsException.class, () -> subjectService.suggestSubject(creator.getId(), "Linear Algebra"));

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    private User newAdmin() {
        User admin = newUser();
        admin.setRole(UserRole.ADMIN);
        return admin;
    }

    @Test
    void shouldApproveSubjectWhenSubjectIsPending() {
        Subject subject = newSubject(SubjectStatus.PENDING);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject approved = subjectService.approveSubject(subject.getId(), admin.getId());

        Assertions.assertEquals(SubjectStatus.APPROVED, approved.getStatus());
        Assertions.assertEquals(admin, approved.getEvaluator());
    }

    @Test
    void shouldThrowSubjectNotFoundExceptionWhenApprovingUnknownSubject() {
        User admin = newAdmin();
        UUID subjectUuid = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(subjectRepository.findById(subjectUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(SubjectNotFoundException.class, () -> subjectService.approveSubject(subjectUuid, admin.getId()));

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    @Test
    void shouldThrowInvalidSubjectStatusExceptionWhenApprovingNonPendingSubject() {
        Subject subject = newSubject(SubjectStatus.REJECTED);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));

        Assertions.assertThrows(InvalidSubjectStatusException.class, () -> subjectService.approveSubject(subject.getId(), admin.getId()));

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenAdminDoesNotExistOnApprove() {
        UUID adminUuid = UUID.randomUUID();

        when(userRepository.findById(adminUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> subjectService.approveSubject(UUID.randomUUID(), adminUuid));

        verifyNoInteractions(subjectRepository);
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenRegularUserApprovesSubject() {
        User user = newUser();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> subjectService.approveSubject(UUID.randomUUID(), user.getId()));

        verifyNoInteractions(subjectRepository);
    }

    @Test
    void shouldRejectSubjectWhenSubjectIsPending() {
        Subject subject = newSubject(SubjectStatus.PENDING);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject rejected = subjectService.rejectSubject(subject.getId(), admin.getId());

        Assertions.assertEquals(SubjectStatus.REJECTED, rejected.getStatus());
        Assertions.assertEquals(admin, rejected.getEvaluator());
    }

    @Test
    void shouldThrowSubjectNotFoundExceptionWhenRejectingUnknownSubject() {
        User admin = newAdmin();
        UUID subjectUuid = UUID.randomUUID();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(subjectRepository.findById(subjectUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(SubjectNotFoundException.class, () -> subjectService.rejectSubject(subjectUuid, admin.getId()));

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    @Test
    void shouldThrowInvalidSubjectStatusExceptionWhenRejectingNonPendingSubject() {
        Subject subject = newSubject(SubjectStatus.APPROVED);
        User admin = newAdmin();

        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));

        Assertions.assertThrows(InvalidSubjectStatusException.class, () -> subjectService.rejectSubject(subject.getId(), admin.getId()));

        verify(subjectRepository, never()).save(any(Subject.class));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenAdminDoesNotExistOnReject() {
        UUID adminUuid = UUID.randomUUID();

        when(userRepository.findById(adminUuid)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> subjectService.rejectSubject(UUID.randomUUID(), adminUuid));

        verifyNoInteractions(subjectRepository);
    }

    @Test
    void shouldThrowUserWithoutPermissionExceptionWhenRegularUserRejectsSubject() {
        User user = newUser();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Assertions.assertThrows(UserWithoutPermissionException.class, () -> subjectService.rejectSubject(UUID.randomUUID(), user.getId()));

        verifyNoInteractions(subjectRepository);
    }

    @Test
    void shouldListEverySubjectByNameWhenNoStatusIsGiven() {
        List<Subject> subjects = List.of(newSubject(SubjectStatus.APPROVED), newSubject(SubjectStatus.PENDING));
        when(subjectRepository.findAllByOrderByNameAsc()).thenReturn(subjects);

        assertEquals(subjects, subjectService.listSubjects(null));
    }

    @Test
    void shouldListOnlySubjectsWithStatusWhenStatusIsGiven() {
        List<Subject> approved = List.of(newSubject(SubjectStatus.APPROVED));
        when(subjectRepository.findByStatusOrderByNameAsc(SubjectStatus.APPROVED)).thenReturn(approved);

        assertEquals(approved, subjectService.listSubjects(SubjectStatus.APPROVED));
        verify(subjectRepository, never()).findAllByOrderByNameAsc();
    }

    @Test
    void shouldReturnSubjectWhenFindingExistingId() {
        Subject subject = newSubject(SubjectStatus.PENDING);
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));

        assertSame(subject, subjectService.findSubject(subject.getId()));
    }

    @Test
    void shouldThrowSubjectNotFoundWhenFindingMissingId() {
        UUID missing = UUID.randomUUID();
        when(subjectRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(SubjectNotFoundException.class, () -> subjectService.findSubject(missing));
    }
}
