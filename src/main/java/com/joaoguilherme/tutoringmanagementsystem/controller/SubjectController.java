package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.dto.SuggestSubjectRequest;
import com.joaoguilherme.tutoringmanagementsystem.dto.SubjectResponse;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse suggestSubject(@Valid @RequestBody SuggestSubjectRequest request, @AuthenticationPrincipal User currentUser) {
        return SubjectResponse.from(subjectService.suggestSubject(currentUser.getId(), request.subjectName()));
    }

    @PatchMapping("/{subjectUuid}/approve")
    public SubjectResponse approveSubject(@PathVariable UUID subjectUuid, @AuthenticationPrincipal User currentUser) {
        return SubjectResponse.from(subjectService.approveSubject(subjectUuid, currentUser.getId()));
    }

    @PatchMapping("/{subjectUuid}/reject")
    public SubjectResponse rejectSubject(@PathVariable UUID subjectUuid, @AuthenticationPrincipal User currentUser) {
        return SubjectResponse.from(subjectService.rejectSubject(subjectUuid, currentUser.getId()));
    }
}
