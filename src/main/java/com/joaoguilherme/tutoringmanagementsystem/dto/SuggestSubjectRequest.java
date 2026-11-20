package com.joaoguilherme.tutoringmanagementsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuggestSubjectRequest(@NotBlank @Size(max = 255) String subjectName) {
}
