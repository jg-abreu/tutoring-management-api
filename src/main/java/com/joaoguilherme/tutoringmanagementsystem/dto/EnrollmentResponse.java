package com.joaoguilherme.tutoringmanagementsystem.dto;

import com.joaoguilherme.tutoringmanagementsystem.model.Enrollment;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.EnrollmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EnrollmentResponse(UUID id, EnrollmentStatus status, Integer queuePosition, SessionResponse session,
                                 UserSummary student, OffsetDateTime createdAt) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(enrollment.getId(), enrollment.getStatus(), enrollment.getQueuePosition(),
                SessionResponse.from(enrollment.getSession()), UserSummary.from(enrollment.getStudent()),
                enrollment.getCreatedAt());
    }
}
