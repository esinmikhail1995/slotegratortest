package com.slotegrator.qa.dto;

/** {@code CredentialsDTO} — request body of {@code POST /api/tester/login}. */
public record CredentialsDto(String email, String password) {
}
