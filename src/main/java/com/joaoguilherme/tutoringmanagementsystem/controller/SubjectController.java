package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.dto.SuggestSubjectRequest;
import com.joaoguilherme.tutoringmanagementsystem.model.Subject;
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
    public Subject suggestSubject(@Valid @RequestBody SuggestSubjectRequest request, @AuthenticationPrincipal User currentUser) {
        return subjectService.suggestSubject(currentUser.getId(), request.subjectName());
    }

    @PatchMapping("/{subjectUuid}/approve")
    public Subject approveSubject(@PathVariable UUID subjectUuid, @AuthenticationPrincipal User currentUser) {
        return subjectService.approveSubject(subjectUuid, currentUser.getId());
    }

    @PatchMapping("/{subjectUuid}/reject")
    public Subject rejectSubject(@PathVariable UUID subjectUuid, @AuthenticationPrincipal User currentUser) {
        return subjectService.rejectSubject(subjectUuid, currentUser.getId());
    }
}
