package com.example.blog.service.impl;

import com.example.blog.dto.UserProfileUpdateRequest;
import com.example.blog.dto.UserRegisterRequest;
import com.example.blog.entity.Role;
import com.example.blog.entity.User;
import com.example.blog.exception.ResourceNotFoundException;
import com.example.blog.repository.*;
import com.example.blog.service.FileStorageService;
import com.example.blog.service.NotificationService;
import com.example.blog.service.UserService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserQuizResultRepository userQuizResultRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () -> {
                                    log.warn(
                                            "Security login failed: User not found with username: {}",
                                            username);
                                    return new UsernameNotFoundException(
                                            "User not found: " + username);
                                });
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> {
                            log.warn("User not found with username: {}", username);
                            return new ResourceNotFoundException(
                                    "User not found with username: " + username);
                        });
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(
                        () -> {
                            log.warn("User not found with ID: {}", id);
                            return new ResourceNotFoundException("User not found with id: " + id);
                        });
    }

    @Override
    @Transactional
    public User register(UserRegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed: Username '{}' is already taken", request.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }

        Role userRole = request.getRole().equalsIgnoreCase("teacher") ? Role.TEACHER : Role.STUDENT;

        User user =
                User.builder()
                        .username(request.getUsername())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .role(userRole)
                        .build();

        User savedUser = userRepository.save(user);
        log.info(
                "Successfully registered new user: '{}' with role: '{}'",
                savedUser.getUsername(),
                savedUser.getRole());
        return savedUser;
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = getById(userId);

        boolean usernameChanged = !user.getUsername().equals(request.getUsername());
        if (usernameChanged) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                log.warn(
                        "Profile update failed: Username '{}' is already taken",
                        request.getUsername());
                throw new IllegalArgumentException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        // Handle password change request
        if (request.getCurrentPassword() != null && !request.getCurrentPassword().isEmpty()) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn(
                        "Profile update failed: Incorrect current password for user '{}'",
                        user.getUsername());
                throw new IllegalArgumentException("Incorrect current password");
            }
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                throw new IllegalArgumentException("New password cannot be empty");
            }
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new IllegalArgumentException("Passwords do not match");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setTitle(request.getTitle());
        user.setBio(request.getBio());
        user.setGithubUrl(request.getGithubUrl());
        user.setLinkedinUrl(request.getLinkedinUrl());

        if (request.getAvatarFile() != null && !request.getAvatarFile().isEmpty()) {
            try {
                String uploadedUrl = fileStorageService.storeFile(request.getAvatarFile());
                if (uploadedUrl != null) {
                    user.setAvatarUrl(uploadedUrl);
                }
            } catch (Exception e) {
                log.error(
                        "Failed to store avatar for user '{}': {}",
                        user.getUsername(),
                        e.getMessage());
                throw new RuntimeException("Failed to store avatar: " + e.getMessage());
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated profile for user: '{}'", updatedUser.getUsername());
        return updatedUser;
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = getById(userId);

        userQuizResultRepository.deleteByUserId(userId);
        userAchievementRepository.deleteByUserId(userId);
        notificationService.clearAll(userId);
        chatMessageRepository.deleteBySenderIdOrRecipientId(userId);
        userRepository.delete(user);

        log.info(
                "Successfully deleted user: '{}' (ID: {}) and all associated data",
                user.getUsername(),
                userId);
    }
}
