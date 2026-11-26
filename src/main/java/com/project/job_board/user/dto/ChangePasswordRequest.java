package com.project.job_board.user.dto;

import com.project.job_board.auth.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangePasswordRequest(@NotBlank String currentPassword, @NotNull @ValidPassword String newPassword) {
}
