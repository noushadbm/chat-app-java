package com.chatapp.client.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class for displaying messages in the chat UI.
 */
public class MessageItem {

    private final StringProperty sender;
    private final StringProperty content;
    private final StringProperty timestamp;
    private final StringProperty type; // "text", "system", "own", "file", "file-own"

    // File specific (nullable for non-file messages)
    private String fileId;
    private String originalFilename;
    private long fileSize;
    private String contentType;

    public MessageItem(String sender, String content, String timestamp, String type) {
        this.sender = new SimpleStringProperty(sender);
        this.content = new SimpleStringProperty(content);
        this.timestamp = new SimpleStringProperty(timestamp);
        this.type = new SimpleStringProperty(type);
    }

    // Constructor for file messages
    public MessageItem(String sender, String timestamp, String type,
                       String fileId, String originalFilename, long fileSize, String contentType) {
        this.sender = new SimpleStringProperty(sender);
        this.content = new SimpleStringProperty(""); // not used for files
        this.timestamp = new SimpleStringProperty(timestamp);
        this.type = new SimpleStringProperty(type);
        this.fileId = fileId;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    public String getSender() {
        return sender.get();
    }

    public StringProperty senderProperty() {
        return sender;
    }

    public String getContent() {
        return content.get();
    }

    public StringProperty contentProperty() {
        return content;
    }

    public String getTimestamp() {
        return timestamp.get();
    }

    public StringProperty timestampProperty() {
        return timestamp;
    }

    public String getType() {
        return type.get();
    }

    public StringProperty typeProperty() {
        return type;
    }

    // File getters
    public String getFileId() {
        return fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getContentType() {
        return contentType;
    }
}