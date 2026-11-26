package com.project.job_board.jobapplication.dto;

import com.project.job_board.jobapplication.ApplicationStatus;
import com.project.job_board.jobapplication.WorkMode;
import com.project.job_board.jobapplication.validation.SalaryRange;
import com.project.job_board.jobapplication.validation.ValidSalaryRange;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating (POST) and fully replacing (PUT) a job application.
 * <p>
 * {@code status} is optional: it defaults to {@code APPLIED} on create and is left unchanged on update.
 */
@ValidSalaryRange
public record JobApplicationRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 200) String company,
        @Size(max = 100) String position,
        ApplicationStatus status,
        @PastOrPresent LocalDate appliedDate,
        @Size(max = 200) String location,
        WorkMode workMode,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal salaryMin,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal salaryMax,
        @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO 4217 currency code, e.g. EUR") String salaryCurrency,
        @Size(max = 2048) @URL(regexp = "^https?://.*", message = "must be a valid http(s) URL") String jobUrl,
        @Size(max = 10_000) String notes
) implements SalaryRange {
}
