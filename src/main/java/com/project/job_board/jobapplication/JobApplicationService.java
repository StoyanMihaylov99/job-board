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

    public Page<JobApplicationResponse> findAll(UUID ownerId, JobApplicationFilter filter, Pageable pageable) {
        return repository.findAll(JobApplicationSpecifications.from(ownerId, filter), pageable)
                .map(JobApplicationMapper::toResponse);
    }

    public JobApplicationResponse findById(UUID ownerId, UUID id) {
        return JobApplicationMapper.toResponse(getOrThrow(ownerId, id));
    }

    @Transactional
    public JobApplicationResponse create(UUID ownerId, JobApplicationRequest request) {
        JobApplication entity = new JobApplication();
        entity.setOwnerId(ownerId);
        JobApplicationMapper.apply(request, entity);
        fillAppliedDateIfMissing(entity);
        return JobApplicationMapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public JobApplicationResponse update(UUID ownerId, UUID id, JobApplicationRequest request) {
        JobApplication entity = getOrThrow(ownerId, id);
        JobApplicationMapper.apply(request, entity);
        fillAppliedDateIfMissing(entity);
        return JobApplicationMapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public JobApplicationResponse updateStatus(UUID ownerId, UUID id, ApplicationStatus status) {
        JobApplication entity = getOrThrow(ownerId, id);
        entity.setStatus(status);
        fillAppliedDateIfMissing(entity);
        return JobApplicationMapper.toResponse(repository.saveAndFlush(entity));
    }

    @Transactional
    public void delete(UUID ownerId, UUID id) {
        repository.delete(getOrThrow(ownerId, id));
    }

    public StatusStatsResponse stats(UUID ownerId) {
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, 0L);
        }
        long total = 0;
        for (StatusCount row : repository.countByStatus(ownerId)) {
            byStatus.put(row.status(), row.count());
            total += row.count();
        }
        return new StatusStatsResponse(total, byStatus);
    }

    /** Someone else's application is reported as not found, so ids can't be probed. */
    private JobApplication getOrThrow(UUID ownerId, UUID id) {
        return repository.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new JobApplicationNotFoundException(id));
    }

    /** Anything past the wishlist stage has been applied to; default the date to today if the user left it blank. */
    private void fillAppliedDateIfMissing(JobApplication entity) {
        if (entity.getStatus() != ApplicationStatus.WISHLIST && entity.getAppliedDate() == null) {
            entity.setAppliedDate(LocalDate.now(clock));
        }
    }
}
