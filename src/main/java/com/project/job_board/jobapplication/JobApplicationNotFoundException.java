package com.project.job_board.jobapplication;

import java.util.UUID;

public class JobApplicationNotFoundException extends RuntimeException {

    public JobApplicationNotFoundException(UUID id) {
        super("Job application with id '%s' was not found".formatted(id));
    }
}
