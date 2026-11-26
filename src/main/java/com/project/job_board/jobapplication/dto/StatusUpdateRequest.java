package com.project.job_board.jobapplication.dto;

import com.project.job_board.jobapplication.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull ApplicationStatus status) {
}
