package com.chatapp.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Login request message from client to server.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRequest extends ChatMessage {

    private String username;
    private String password;

    public LoginRequest() {
        super();
    }

    public LoginRequest(String username, String password) {
        super();
        this.username = username;
        this.password = password;
    }

    @Override
    public String getMessageType() {
        return "login";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}