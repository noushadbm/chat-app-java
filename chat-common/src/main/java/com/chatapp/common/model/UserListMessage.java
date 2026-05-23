package com.chatapp.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Message containing the list of currently active users.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserListMessage extends ChatMessage {

    private List<String> users;
    private Map<String, String> displayNames;
    private List<String> onlineUsers;

    public UserListMessage() {
        super();
        this.displayNames = new HashMap<>();
        this.onlineUsers = new ArrayList<>();
    }

    public UserListMessage(List<String> users) {
        super();
        this.users = users;
        this.displayNames = new HashMap<>();
        this.onlineUsers = new ArrayList<>();
    }

    public UserListMessage(List<String> users, Map<String, String> displayNames) {
        super();
        this.users = users;
        this.displayNames = displayNames != null ? displayNames : new HashMap<>();
        this.onlineUsers = new ArrayList<>();
    }

    public UserListMessage(List<String> users, Map<String, String> displayNames, List<String> onlineUsers) {
        super();
        this.users = users;
        this.displayNames = displayNames != null ? displayNames : new HashMap<>();
        this.onlineUsers = onlineUsers != null ? onlineUsers : new ArrayList<>();
    }

    @Override
    public String getMessageType() {
        return "user_list";
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public Map<String, String> getDisplayNames() {
        return displayNames;
    }

    public void setDisplayNames(Map<String, String> displayNames) {
        this.displayNames = displayNames;
    }

    /**
     * Get display name for a specific user.
     */
    public String getDisplayName(String username) {
        return displayNames != null ? displayNames.get(username) : username;
    }

    public List<String> getOnlineUsers() {
        return onlineUsers;
    }

    public void setOnlineUsers(List<String> onlineUsers) {
        this.onlineUsers = onlineUsers != null ? onlineUsers : new ArrayList<>();
    }
}