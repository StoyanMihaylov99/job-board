package com.project.job_board.jobapplication;

import com.project.job_board.jobapplication.dto.JobApplicationFilter;
import com.project.job_board.jobapplication.dto.JobApplicationRequest;
import com.project.job_board.jobapplication.dto.JobApplicationResponse;
import com.project.job_board.jobapplication.dto.StatusStatsResponse;
import com.project.job_board.jobapplication.dto.StatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    private final JobApplicationRepository repository;
    private final Clock clock;

    public JobApplicationService(JobApplicationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Page<JobApplicationResponse> findAll(JobApplicationFilter filter, Pageable pageable) {
        return repository.findAll(JobApplicationSpecifications.from(filter), pageable)
                .map(JobApplicationMapper::toResponse);
    }

    public JobApplicationResponse findById(UUID id) {
        return JobApplicationMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public JobApplicationResponse create(JobApplicationRequest request) {
        JobApplication entity = new JobApplication();
        JobApplicationMapper.apply(request, entity);
        fillAppliedDateIfMissing(entity);
        return JobApplicationMapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public JobApplicationResponse update(UUID id, JobApplicationRequest request) {
        JobApplication entity = getOrThrow(id);
        JobApplicationMapper.apply(request, entity);
        fillAppliedDateIfMissing(entity);
        return JobApplicationMapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public JobApplicationResponse updateStatus(UUID id, ApplicationStatus status) {
        JobApplication entity = getOrThrow(id);
        entity.setStatus(status);
        fillAppliedDateIfMissing(entity);
        return JobApplicationMapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(getOrThrow(id));
    }

    public StatusStatsResponse stats() {
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, 0L);
        }
        long total = 0;
        for (StatusCount row : repository.countByStatus()) {
            byStatus.put(row.status(), row.count());
            total += row.count();
        }
        return new StatusStatsResponse(total, byStatus);
    }

    private JobApplication getOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new JobApplicationNotFoundException(id));
    }

    /** Anything past the wishlist stage has been applied to; default the date to today if the user left it blank. */
    private void fillAppliedDateIfMissing(JobApplication entity) {
        if (entity.getStatus() != ApplicationStatus.WISHLIST && entity.getAppliedDate() == null) {
            entity.setAppliedDate(LocalDate.now(clock));
        }
    }
}
