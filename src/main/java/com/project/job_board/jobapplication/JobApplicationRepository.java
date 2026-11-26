package com.project.job_board.jobapplication;

import com.project.job_board.jobapplication.dto.StatusCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, UUID>, JpaSpecificationExecutor<JobApplication> {

    Optional<JobApplication> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("""
            select new com.project.job_board.jobapplication.dto.StatusCount(j.status, count(j))
            from JobApplication j
            where j.ownerId = :ownerId
            group by j.status
            """)
    List<StatusCount> countByStatus(@Param("ownerId") UUID ownerId);
}
