package com.project.job_board.auth.dto;

import com.project.job_board.auth.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @ValidPassword String password,
        @NotBlank @Size(max = 100) String displayName
) {

    /** Surrounding whitespace (common with autofill and paste) is not part of the address. */
    public RegisterRequest {
        email = email == null ? null : email.trim();
    }
}
