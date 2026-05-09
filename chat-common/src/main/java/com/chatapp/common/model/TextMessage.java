package com.chatapp.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a text chat message sent between users.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextMessage extends ChatMessage {

    private String sender;
    private String content;
    private String recipient; // null for broadcast
    private String messageId; // Optional ID for message identification

    public TextMessage() {
        super();
    }

    public TextMessage(String sender, String content) {
        super();
        this.sender = sender;
        this.content = content;
    }

    public TextMessage(String sender, String content, String recipient) {
        super();
        this.sender = sender;
        this.content = content;
        this.recipient = recipient;
    }

    @Override
    public String getMessageType() {
        return "text";
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}