package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.dto.TutoringBondRequest;
import com.joaoguilherme.tutoringmanagementsystem.model.TutoringBond;
import com.joaoguilherme.tutoringmanagementsystem.model.User;
import com.joaoguilherme.tutoringmanagementsystem.service.TutoringBondService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tutoring-bonds")
public class TutoringBondController {

    private final TutoringBondService tutoringBondService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TutoringBond requestTutoringBond(@Valid @RequestBody TutoringBondRequest request, @AuthenticationPrincipal User currentUser) {
        return tutoringBondService.requestTutoringBond(request.subjectUuid(), currentUser.getId());
    }

    @PatchMapping("/{tutoringBondUuid}/approve")
    public TutoringBond approveTutoringBond(@PathVariable UUID tutoringBondUuid, @AuthenticationPrincipal User currentUser) {
        return tutoringBondService.approveTutoringBond(tutoringBondUuid, currentUser.getId());
    }

    @PatchMapping("/{tutoringBondUuid}/reject")
    public TutoringBond rejectTutoringBond(@PathVariable UUID tutoringBondUuid, @AuthenticationPrincipal User currentUser) {
        return tutoringBondService.rejectTutoringBond(tutoringBondUuid, currentUser.getId());
    }

    @PatchMapping("/{tutoringBondUuid}/revoke")
    public TutoringBond revokeTutoringBond(@PathVariable UUID tutoringBondUuid, @AuthenticationPrincipal User currentUser) {
        return tutoringBondService.revokeTutoringBond(tutoringBondUuid, currentUser.getId());
    }
}
