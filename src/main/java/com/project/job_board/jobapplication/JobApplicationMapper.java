package com.project.job_board.jobapplication;

import com.project.job_board.jobapplication.dto.JobApplicationRequest;
import com.project.job_board.jobapplication.dto.JobApplicationResponse;
import org.springframework.util.StringUtils;

final class JobApplicationMapper {

    private JobApplicationMapper() {
    }

    static JobApplicationResponse toResponse(JobApplication entity) {
        return new JobApplicationResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getCompany(),
                entity.getPosition(),
                entity.getStatus(),
                entity.getAppliedDate(),
                entity.getLocation(),
                entity.getWorkMode(),
                entity.getSalaryMin(),
                entity.getSalaryMax(),
                entity.getSalaryCurrency(),
                entity.getJobUrl(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    /** Copies all editable fields from the request onto the entity. A null status leaves the current status as is. */
    static void apply(JobApplicationRequest request, JobApplication entity) {
        entity.setTitle(request.title().trim());
        entity.setCompany(request.company().trim());
        entity.setPosition(trimToNull(request.position()));
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        entity.setAppliedDate(request.appliedDate());
        entity.setLocation(trimToNull(request.location()));
        entity.setWorkMode(request.workMode());
        entity.setSalaryMin(request.salaryMin());
        entity.setSalaryMax(request.salaryMax());
        entity.setSalaryCurrency(trimToNull(request.salaryCurrency()));
        entity.setJobUrl(trimToNull(request.jobUrl()));
        entity.setNotes(trimToNull(request.notes()));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
