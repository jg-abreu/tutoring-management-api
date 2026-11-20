package com.joaoguilherme.tutoringmanagementsystem.dto;

import com.joaoguilherme.tutoringmanagementsystem.model.Session;
import com.joaoguilherme.tutoringmanagementsystem.model.enums.SessionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionResponse(UUID id, OffsetDateTime startTime, OffsetDateTime endTime, int spots, boolean allowsWaitlist,
                              SessionStatus status, TutoringBondResponse bond, OffsetDateTime createdAt) {

    public static SessionResponse from(Session session) {
        return new SessionResponse(session.getId(), session.getStartTime(), session.getEndTime(), session.getSpots(),
                session.isAllowsWaitlist(), session.getStatus(), TutoringBondResponse.from(session.getBond()),
                session.getCreatedAt());
    }
}
