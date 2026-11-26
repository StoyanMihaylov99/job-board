package com.project.job_board.jobapplication.dto;

import com.project.job_board.jobapplication.ApplicationStatus;
import com.project.job_board.jobapplication.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobApplicationResponse(
        UUID id,
        String title,
        String company,
        String position,
        ApplicationStatus status,
        LocalDate appliedDate,
        String location,
        WorkMode workMode,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String jobUrl,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
