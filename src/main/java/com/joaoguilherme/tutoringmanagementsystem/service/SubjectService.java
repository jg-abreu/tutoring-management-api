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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    private final UserRepository userRepository;

    public Subject suggestSubject(UUID userUuid, String subjectName) {
        User creator = userRepository.findById(userUuid).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (subjectRepository.existsByName(subjectName)) {
            throw new SubjectAlreadyExistsException("Subject already exists");
        }

        Subject subject = new Subject(creator, subjectName);

        return subjectRepository.save(subject);
    }

    public Subject approveSubject(UUID subjectUuid, UUID adminUuid) {
        User admin = findAdmin(adminUuid);

        Subject subject = subjectRepository.findById(subjectUuid).orElseThrow(() -> new SubjectNotFoundException("Subject not found"));

        if (!(subject.getStatus().equals(SubjectStatus.PENDING))) {
            throw new InvalidSubjectStatusException("Invalid subject status");
        }
        subject.setStatus(SubjectStatus.APPROVED);

        subject.setEvaluator(admin);

        return subjectRepository.save(subject);
    }

    public Subject rejectSubject(UUID subjectUuid, UUID adminUuid) {
        User admin = findAdmin(adminUuid);

        Subject subject = subjectRepository.findById(subjectUuid).orElseThrow(() -> new SubjectNotFoundException("Subject not found"));

        if (!(subject.getStatus().equals(SubjectStatus.PENDING))) {
            throw new InvalidSubjectStatusException("Invalid subject exception");
        }

        subject.setStatus(SubjectStatus.REJECTED);

        subject.setEvaluator(admin);

        return subjectRepository.save(subject);
    }

    @Transactional(readOnly = true)
    public List<Subject> listSubjects(SubjectStatus status) {
        return status == null ? subjectRepository.findAllByOrderByNameAsc() : subjectRepository.findByStatusOrderByNameAsc(status);
    }

    @Transactional(readOnly = true)
    public Subject findSubject(UUID subjectUuid) {
        return subjectRepository.findById(subjectUuid).orElseThrow(() -> new SubjectNotFoundException("Subject not found"));
    }

    private User findAdmin(UUID userUuid) {
        User user = userRepository.findById(userUuid).orElseThrow(() -> new UserNotFoundException("User not found"));
        if (user.getRole() != UserRole.ADMIN) {
            throw new UserWithoutPermissionException("User without permission");
        }
        return user;
    }
}
