package com.coslavko.multilegged.dto;

public record ContactRequest(String name,
    String email,
    String subject,
    String message) {
}
