package com.example.blog.service;

import com.example.blog.dto.UserProfileUpdateRequest;
import com.example.blog.dto.UserRegisterRequest;
import com.example.blog.entity.User;
import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service interface for managing user authentication, profile updates, and lifecycle operations.
 */
public interface UserService extends UserDetailsService {

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an Optional containing the found user, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Gets a user by their username. Throws an exception if not found.
     *
     * @param username the username to search for
     * @return the user
     * @throws com.example.blog.exception.ResourceNotFoundException if the user is not found
     */
    User getByUsername(String username);

    /**
     * Gets a user by their ID. Throws an exception if not found.
     *
     * @param id the user ID
     * @return the user
     * @throws com.example.blog.exception.ResourceNotFoundException if the user is not found
     */
    User getById(Long id);

    /**
     * Registers a new user with the specified request details.
     *
     * @param request the registration details
     * @return the newly created user
     */
    User register(UserRegisterRequest request);

    /**
     * Updates an existing user's profile metadata and handles optional password changes.
     *
     * @param userId the user ID to update
     * @param request the profile update details
     * @return the updated user
     */
    User updateProfile(Long userId, UserProfileUpdateRequest request);

    /**
     * Deletes a user by their ID and cleans up their associated data.
     *
     * @param userId the user ID to delete
     */
    void deleteUser(Long userId);
}
