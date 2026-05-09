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
    private final StringProperty type; // "text", "system", "own"

    public MessageItem(String sender, String content, String timestamp, String type) {
        this.sender = new SimpleStringProperty(sender);
        this.content = new SimpleStringProperty(content);
        this.timestamp = new SimpleStringProperty(timestamp);
        this.type = new SimpleStringProperty(type);
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
}