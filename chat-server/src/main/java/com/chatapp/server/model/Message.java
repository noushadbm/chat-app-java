package com.chatapp.server.model;

import jakarta.persistence.*;

/**
 * Message entity for storing chat messages in SQLite database.
 * Stores both group chat and P2P messages.
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String sender;

    @Column(nullable = false)
    private String content;

    @Column(length = 50)
    private String recipient; // null for group/broadcast messages

    @Column(nullable = false)
    private Long timestamp;

    @Column(name = "message_type", length = 20)
    private String messageType; // "text", "system"

    @Column(name = "system_message_type", length = 30)
    private String systemMessageType; // "USER_JOINED", "USER_LEFT", "GENERAL", etc. (null for text messages)

    public Message() {
    }

    public Message(String sender, String content, String recipient, Long timestamp, String messageType) {
        this(sender, content, recipient, timestamp, messageType, null);
    }

    public Message(String sender, String content, String recipient, Long timestamp, String messageType, String systemMessageType) {
        this.sender = sender;
        this.content = content;
        this.recipient = recipient;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.systemMessageType = systemMessageType;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSystemMessageType() {
        return systemMessageType;
    }

    public void setSystemMessageType(String systemMessageType) {
        this.systemMessageType = systemMessageType;
    }

    /**
     * Check if this is a group message (no recipient).
     */
    public boolean isGroupMessage() {
        return recipient == null || recipient.isEmpty();
    }
}