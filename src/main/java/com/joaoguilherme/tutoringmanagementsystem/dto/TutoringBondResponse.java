package com.joaoguilherme.tutoringmanagementsystem.dto;

import com.joaoguilherme.tutoringmanagementsystem.model.TutoringBond;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.BondStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TutoringBondResponse(UUID id, BondStatus status, UserSummary requester, UserSummary evaluator,
                                   SubjectResponse subject, OffsetDateTime createdAt) {

    public static TutoringBondResponse from(TutoringBond bond) {
        return new TutoringBondResponse(bond.getId(), bond.getStatus(), UserSummary.from(bond.getRequester()),
                UserSummary.from(bond.getEvaluator()), SubjectResponse.from(bond.getSubject()), bond.getCreatedAt());
    }
}
