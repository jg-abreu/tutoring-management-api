package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.model.Enrollment;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final SessionService sessionService;

    @PatchMapping("/{enrollmentUuid}/cancel")
    public Enrollment cancelEnrollment(@PathVariable UUID enrollmentUuid, @AuthenticationPrincipal User currentUser) {
        return sessionService.cancelEnrollment(enrollmentUuid, currentUser.getId());
    }
}
