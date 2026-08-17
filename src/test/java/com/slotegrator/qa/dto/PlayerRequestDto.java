package com.slotegrator.qa.dto;

/**
 * {@code PlayerRequestDTO} — request body of {@code POST /api/automationTask/create}.
 *
 * <p>Serialised in snake_case ({@code currency_code}, {@code password_change}, {@code password_repeat})
 * by the shared {@link com.slotegrator.qa.http.Json} mapper.
 */
public record PlayerRequestDto(
        String currencyCode,
        String email,
        String name,
        String surname,
        String username,
        String passwordChange,
        String passwordRepeat) {
}
