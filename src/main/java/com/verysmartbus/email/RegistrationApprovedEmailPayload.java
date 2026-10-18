package com.verysmartbus.email;

/** Immutable, stored template data for a registration-approval email. */
public record RegistrationApprovedEmailPayload(
        String name,
        String username,
        String email,
        String loginUrl
) {
}
