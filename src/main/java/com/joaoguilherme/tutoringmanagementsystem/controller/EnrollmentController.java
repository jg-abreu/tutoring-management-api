package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.dto.EnrollmentResponse;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final SessionService sessionService;

    @GetMapping
    public List<EnrollmentResponse> listOwnEnrollments(@AuthenticationPrincipal User currentUser) {
        return sessionService.listUserEnrollments(currentUser.getId()).stream().map(EnrollmentResponse::from).toList();
    }

    @PatchMapping("/{enrollmentUuid}/cancel")
    public EnrollmentResponse cancelEnrollment(@PathVariable UUID enrollmentUuid, @AuthenticationPrincipal User currentUser) {
        return EnrollmentResponse.from(sessionService.cancelEnrollment(enrollmentUuid, currentUser.getId()));
    }
}
