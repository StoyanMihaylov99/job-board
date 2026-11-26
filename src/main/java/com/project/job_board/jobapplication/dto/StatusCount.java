package com.project.job_board.jobapplication.dto;

import com.project.job_board.jobapplication.ApplicationStatus;

public record StatusCount(ApplicationStatus status, long count) {
}
