package com.chatapp.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * System message for notifications like user joined/left.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemMessage extends ChatMessage {

    private String content;

    @JsonProperty("messageTypeEnum")
    private SystemMessageType systemMessageType;

    public enum SystemMessageType {
        USER_JOINED,
        USER_LEFT,
        SERVER_SHUTDOWN,
        GENERAL
    }

    public SystemMessage() {
        super();
    }

    public SystemMessage(String content, SystemMessageType systemMessageType) {
        super();
        this.content = content;
        this.systemMessageType = systemMessageType;
    }

    @Override
    public String getMessageType() {
        return "system";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public SystemMessageType getMessageTypeEnum() {
        return systemMessageType;
    }

    public void setMessageTypeEnum(SystemMessageType systemMessageType) {
        this.systemMessageType = systemMessageType;
    }
}