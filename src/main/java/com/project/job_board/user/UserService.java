package com.project.job_board.user;

import com.project.job_board.auth.AuthService;
import com.project.job_board.user.dto.ChangePasswordRequest;
import com.project.job_board.user.dto.UpdateProfileRequest;
import com.project.job_board.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder, AuthService authService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public UserResponse get(UUID userId) {
        return UserResponse.from(getOrThrow(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        AppUser user = getOrThrow(userId);
        user.setDisplayName(request.displayName().trim());
        return UserResponse.from(users.saveAndFlush(user));
    }

    /** Changing the password signs out every other device; the caller gets a fresh session. */
    @Transactional
    public AuthService.Session changePassword(UUID userId, ChangePasswordRequest request) {
        AppUser user = getOrThrow(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.saveAndFlush(user);
        return authService.restartSessions(user);
    }

    private AppUser getOrThrow(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
