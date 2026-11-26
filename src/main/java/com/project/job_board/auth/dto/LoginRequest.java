package com.project.job_board.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {

    /** Surrounding whitespace (common with autofill and paste) is not part of the address. */
    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
