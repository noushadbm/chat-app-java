package com.chatapp.server.model;

import jakarta.persistence.*;

/**
 * User entity for chat application.
 * Stored in SQLite database.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at")
    private Long createdAt;

    @Column(name = "is_active")
    private Boolean isActive;

    public User() {
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    public User(String username, String password) {
        this.username = username;
        this.displayName = username; // Default display name is username
        this.password = password;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    public User(String username, String password, String displayName) {
        this.username = username;
        this.displayName = displayName != null && !displayName.isEmpty() ? displayName : username;
        this.password = password;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}