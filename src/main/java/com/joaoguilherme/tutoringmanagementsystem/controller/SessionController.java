package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.dto.SessionRequest;
import com.joaoguilherme.tutoringmanagementsystem.dto.EnrollmentResponse;
import com.joaoguilherme.tutoringmanagementsystem.dto.SessionResponse;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(@Valid @RequestBody SessionRequest request, @AuthenticationPrincipal User currentUser) {
        return SessionResponse.from(sessionService.createSession(request.tutoringBondUuid(), currentUser.getId(), request.startTime(), request.endTime(), request.spots(), request.allowsWaitlist()));
    }

    @PatchMapping("/{sessionUuid}/cancel")
    public SessionResponse cancelSession(@PathVariable UUID sessionUuid, @AuthenticationPrincipal User currentUser) {
        return SessionResponse.from(sessionService.cancelSession(sessionUuid, currentUser.getId()));
    }

    @PostMapping("/{sessionUuid}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable UUID sessionUuid, @AuthenticationPrincipal User currentUser) {
        return EnrollmentResponse.from(sessionService.enroll(sessionUuid, currentUser.getId()));
    }
}
