package com.joaoguilherme.tutoringmanagementsystem.dto;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, Instant expiresAt) {
}
