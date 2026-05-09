package com.chatapp.server.repository;

import com.chatapp.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     *
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if a username already exists.
     *
     * @param username The username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Find all active users.
     *
     * @return Iterable of active users
     */
    Iterable<User> findByIsActiveTrue();
}