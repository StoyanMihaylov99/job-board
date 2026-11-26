package com.project.job_board.auth;

/** Login or refresh failed. The message is safe to show to the user and never reveals which part was wrong. */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
