package com.chatapp.server.service;

import com.chatapp.server.model.User;
import com.chatapp.server.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing chat users.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new user with encoded password.
     *
     * @param username The username
     * @param password The plain text password
     * @return The created user
     */
    public User createUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(username, encodedPassword);
        return userRepository.save(user);
    }

    /**
     * Validate user credentials.
     *
     * @param username The username
     * @param password The plain text password
     * @return Optional containing the user if valid
     */
    public Optional<User> validateCredentials(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getIsActive() && passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    /**
     * Get all active users.
     *
     * @return List of active users
     */
    public List<User> getAllActiveUsers() {
        return (List<User>) userRepository.findByIsActiveTrue();
    }

    /**
     * Get user by username.
     *
     * @param username The username
     * @return Optional containing the user if found
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Deactivate a user account.
     *
     * @param username The username to deactivate
     * @return true if successful
     */
    public boolean deactivateUser(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsActive(false);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Activate a user account.
     *
     * @param username The username to activate
     * @return true if successful
     */
    public boolean activateUser(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsActive(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Delete a user by ID.
     *
     * @param id The user ID
     */
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Check if username exists.
     *
     * @param username The username to check
     * @return true if exists
     */
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
}