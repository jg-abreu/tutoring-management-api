package com.joaoguilherme.tutoringmanagementsystem.controller;

import com.joaoguilherme.tutoringmanagementsystem.dto.TutoringBondRequest;
import com.joaoguilherme.tutoringmanagementsystem.dto.TutoringBondResponse;
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
    public TutoringBondResponse requestTutoringBond(@Valid @RequestBody TutoringBondRequest request, @AuthenticationPrincipal User currentUser) {
        return TutoringBondResponse.from(tutoringBondService.requestTutoringBond(request.subjectUuid(), currentUser.getId()));
    }

    @PatchMapping("/{tutoringBondUuid}/approve")
    public TutoringBondResponse approveTutoringBond(@PathVariable UUID tutoringBondUuid, @AuthenticationPrincipal User currentUser) {
        return TutoringBondResponse.from(tutoringBondService.approveTutoringBond(tutoringBondUuid, currentUser.getId()));
    }

    @PatchMapping("/{tutoringBondUuid}/reject")
    public TutoringBondResponse rejectTutoringBond(@PathVariable UUID tutoringBondUuid, @AuthenticationPrincipal User currentUser) {
        return TutoringBondResponse.from(tutoringBondService.rejectTutoringBond(tutoringBondUuid, currentUser.getId()));
    }

    @PatchMapping("/{tutoringBondUuid}/revoke")
    public TutoringBondResponse revokeTutoringBond(@PathVariable UUID tutoringBondUuid, @AuthenticationPrincipal User currentUser) {
        return TutoringBondResponse.from(tutoringBondService.revokeTutoringBond(tutoringBondUuid, currentUser.getId()));
    }
}
