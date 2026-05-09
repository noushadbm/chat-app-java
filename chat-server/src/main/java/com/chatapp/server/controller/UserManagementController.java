package com.chatapp.server.controller;

import com.chatapp.server.model.User;
import com.chatapp.server.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for web-based user management interface.
 */
@Controller
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Home page - redirect to user management.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/users";
    }

    /**
     * User management page - list all users.
     */
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.getAllActiveUsers();
        model.addAttribute("users", users);
        return "users";
    }

    /**
     * Show create user form.
     */
    @GetMapping("/users/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "user-form";
    }

    /**
     * Create a new user.
     */
    @PostMapping("/users")
    public String createUser(@RequestParam String username, @RequestParam String password, Model model) {
        try {
            userService.createUser(username, password);
            return "redirect:/users?success";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", new User());
            return "user-form";
        }
    }

    /**
     * Show edit user form.
     */
    @GetMapping("/users/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        // For now, just show the form - would need findById in service
        model.addAttribute("username", "User " + id);
        return "user-edit";
    }

    /**
     * Delete a user (deactivate).
     */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/users?deleted";
    }

    /**
     * Toggle user active status.
     */
    @PostMapping("/users/{username}/toggle")
    public String toggleUserStatus(@PathVariable String username, @RequestParam boolean active) {
        if (active) {
            userService.activateUser(username);
        } else {
            userService.deactivateUser(username);
        }
        return "redirect:/users?updated";
    }
}