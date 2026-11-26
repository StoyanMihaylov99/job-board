package com.project.job_board.jobapplication.dto;

import com.project.job_board.jobapplication.ApplicationStatus;

import java.util.Map;

/**
 * Dashboard summary: total number of applications and a count for every status (zero-filled).
 */
public record StatusStatsResponse(long total, Map<ApplicationStatus, Long> byStatus) {
}
