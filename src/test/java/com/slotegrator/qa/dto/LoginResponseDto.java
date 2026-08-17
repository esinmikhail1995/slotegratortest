package com.slotegrator.qa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Actual response of {@code POST /api/tester/login}: {@code {"user": {...}, "accessToken": "..."}}.
 *
 * <p>The contract documents a {@code TokenDTO} ({@code access_token}, {@code token_type}, {@code expires_in},
 * {@code scope}) instead — see {@code ContractConformanceTest}. {@code accessToken} is camelCase on the wire
 * while the player payloads are snake_case, hence the explicit {@code @JsonProperty}.
 */
public record LoginResponseDto(
        UserDto user,
        @JsonProperty("accessToken") String accessToken) {

    /** Value for the {@code Authorization} header of the secured endpoints. */
    public String authorizationHeader() {
        return "Bearer " + accessToken;
    }
}
