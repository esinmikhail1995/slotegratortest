package com.slotegrator.qa.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * A player as the API returns it from create / getOne / getAll / deleteOne.
 *
 * <p>The identifier is a 24-character hex string, not the {@code integer} the contract declares, and the
 * field is named {@code _id} by create and deleteOne but {@code id} by getOne and getAll — {@code @JsonAlias}
 * covers both. See {@code ContractConformanceTest} for the full list of deviations.
 */
public record PlayerResponseDto(
        @JsonAlias("_id") String id,
        String username,
        String email,
        String name,
        String surname,
        String currencyCode) {
}
