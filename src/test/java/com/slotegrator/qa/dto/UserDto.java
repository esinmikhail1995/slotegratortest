package com.slotegrator.qa.dto;

/**
 * The tester account returned alongside the access token. Only the fields the tests care about are mapped;
 * the rest of the payload is ignored.
 */
public record UserDto(String id, String email, String name, String surname, String status) {
}
