package com.project.job_board.jobapplication.dto;

import com.project.job_board.jobapplication.ApplicationStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Optional filters for listing job applications. All criteria are combined with AND.
 *
 * @param status      one or more statuses (e.g. {@code ?status=APPLIED&status=INTERVIEWING})
 * @param company     case-insensitive "contains" match on company
 * @param q           case-insensitive free-text search over title, company and position
 * @param appliedFrom applied on or after this date (ISO yyyy-MM-dd)
 * @param appliedTo   applied on or before this date (ISO yyyy-MM-dd)
 */
public record JobApplicationFilter(
        List<ApplicationStatus> status,
        String company,
        String q,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedTo
) {
}
