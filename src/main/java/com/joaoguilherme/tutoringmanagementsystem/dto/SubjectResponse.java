package com.joaoguilherme.tutoringmanagementsystem.dto;

import com.joaoguilherme.tutoringmanagementsystem.model.Subject;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SubjectStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SubjectResponse(UUID id, String name, SubjectStatus status, UserSummary creator, UserSummary evaluator,
                              OffsetDateTime createdAt) {

    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName(), subject.getStatus(),
                UserSummary.from(subject.getCreator()), UserSummary.from(subject.getEvaluator()), subject.getCreatedAt());
    }
}
