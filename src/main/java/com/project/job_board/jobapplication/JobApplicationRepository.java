package com.project.job_board.jobapplication;

import com.project.job_board.jobapplication.dto.StatusCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, UUID>, JpaSpecificationExecutor<JobApplication> {

    @Query("""
            select new com.project.job_board.jobapplication.dto.StatusCount(j.status, count(j))
            from JobApplication j
            group by j.status
            """)
    List<StatusCount> countByStatus();
}
