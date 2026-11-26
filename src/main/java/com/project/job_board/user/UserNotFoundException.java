package com.project.job_board.user;

import java.util.UUID;

/** The authenticated token refers to a user that no longer exists. */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("User '%s' no longer exists".formatted(id));
    }
}
