package com.chatapp.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Message containing the list of currently active users.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserListMessage extends ChatMessage {

    private List<String> users;

    public UserListMessage() {
        super();
    }

    public UserListMessage(List<String> users) {
        super();
        this.users = users;
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
}