package com.coslavko.multilegged.dto;

import jakarta.validation.constraints.NotBlank;

public record ContactRequest(String name,
    @NotBlank String email,
    @NotBlank String subject,
    @NotBlank String message) {
}
