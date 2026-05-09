package com.chatapp.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Login response message from server to client.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse extends ChatMessage {

    private boolean success;
    private String message;
    private String username;

    public LoginResponse() {
        super();
    }

    public LoginResponse(boolean success, String message) {
        super();
        this.success = success;
        this.message = message;
    }

    public LoginResponse(boolean success, String message, String username) {
        super();
        this.success = success;
        this.message = message;
        this.username = username;
    }

    @Override
    public String getMessageType() {
        return "login_response";
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}