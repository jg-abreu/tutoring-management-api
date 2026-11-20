package com.joaoguilherme.tutoringmanagementsystem.dto;

import com.joaoguilherme.tutoringmanagementsystem.model.enums.UserRole;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, Instant expiresAt, UserRole role) {
}
