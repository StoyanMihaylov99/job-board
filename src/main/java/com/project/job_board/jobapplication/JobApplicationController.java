package com.project.job_board.jobapplication;

import com.project.job_board.auth.CurrentUserId;
import com.project.job_board.jobapplication.dto.JobApplicationFilter;
import com.project.job_board.jobapplication.dto.JobApplicationRequest;
import com.project.job_board.jobapplication.dto.JobApplicationResponse;
import com.project.job_board.jobapplication.dto.StatusStatsResponse;
import com.project.job_board.jobapplication.dto.StatusUpdateRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public PagedModel<JobApplicationResponse> list(
            @CurrentUserId UUID userId,
            @ParameterObject JobApplicationFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return new PagedModel<>(service.findAll(userId, filter, pageable));
    }

    @GetMapping("/stats")
    public StatusStatsResponse stats(@CurrentUserId UUID userId) {
        return service.stats(userId);
    }

    @GetMapping("/{id}")
    public JobApplicationResponse get(@CurrentUserId UUID userId, @PathVariable UUID id) {
        return service.findById(userId, id);
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(@CurrentUserId UUID userId,
                                                         @Valid @RequestBody JobApplicationRequest request) {
        JobApplicationResponse created = service.create(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public JobApplicationResponse update(@CurrentUserId UUID userId, @PathVariable UUID id,
                                         @Valid @RequestBody JobApplicationRequest request) {
        return service.update(userId, id, request);
    }

    @PatchMapping("/{id}/status")
    public JobApplicationResponse updateStatus(@CurrentUserId UUID userId, @PathVariable UUID id,
                                               @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(userId, id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUserId UUID userId, @PathVariable UUID id) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
