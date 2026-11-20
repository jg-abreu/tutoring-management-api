package com.joaoguilherme.tutoringmanagementsystem.dto;

import com.joaoguilherme.tutoringmanagementsystem.model.User;

import java.util.UUID;

public record UserSummary(UUID id, String email) {

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getEmail());
    }
}
