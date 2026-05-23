package com.chatapp.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a file that was shared in the chat (group or P2P).
 * The actual file content is not sent over WebSocket.
 * Only metadata is sent; the file is downloaded via HTTP.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileMessage extends ChatMessage {

    private String sender;
    private String recipient;          // null for group chat
    private String fileId;             // Unique ID on server
    private String originalFilename;
    private long size;                 // in bytes
    private String contentType;        // e.g. "application/pdf", "image/png"

    public FileMessage() {
        super();
    }

    public FileMessage(String sender, String recipient, String fileId,
                       String originalFilename, long size, String contentType) {
        super();
        this.sender = sender;
        this.recipient = recipient;
        this.fileId = fileId;
        this.originalFilename = originalFilename;
        this.size = size;
        this.contentType = contentType;
    }

    @Override
    public String getMessageType() {
        return "file";
    }

    // Getters and Setters

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
