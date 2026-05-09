package com.chatapp.server.controller;

import com.chatapp.server.model.User;
import com.chatapp.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for user management.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get all active users.
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllActiveUsers();
        // Don't return passwords
        users.forEach(u -> u.setPassword(""));
        return ResponseEntity.ok(users);
    }

    /**
     * Create a new user.
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password required"));
        }

        try {
            User user = userService.createUser(username, password);
            user.setPassword("");
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if username exists.
     */
    @GetMapping("/check/{username}")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@PathVariable String username) {
        boolean exists = userService.usernameExists(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Deactivate a user.
     */
    @PostMapping("/{username}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable String username) {
        boolean success = userService.deactivateUser(username);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "User deactivated"));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Activate a user.
     */
    @PostMapping("/{username}/activate")
    public ResponseEntity<?> activateUser(@PathVariable String username) {
        boolean success = userService.activateUser(username);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "User activated"));
        }
        return ResponseEntity.notFound().build();
    }
}