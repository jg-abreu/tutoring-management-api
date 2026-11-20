package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.dto.SessionRequest;
import com.joaoguilherme.tutoringmanagementsystem.model.Enrollment;
import com.joaoguilherme.tutoringmanagementsystem.model.Session;
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
    public Session createSession(@Valid @RequestBody SessionRequest request, @AuthenticationPrincipal User currentUser) {
        return sessionService.createSession(request.tutoringBondUuid(), currentUser.getId(), request.startTime(), request.endTime(), request.spots(), request.allowsWaitlist());
    }

    @PatchMapping("/{sessionUuid}/cancel")
    public Session cancelSession(@PathVariable UUID sessionUuid, @AuthenticationPrincipal User currentUser) {
        return sessionService.cancelSession(sessionUuid, currentUser.getId());
    }

    @PostMapping("/{sessionUuid}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public Enrollment enroll(@PathVariable UUID sessionUuid, @AuthenticationPrincipal User currentUser) {
        return sessionService.enroll(sessionUuid, currentUser.getId());
    }
}
