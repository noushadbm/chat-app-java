package com.chatapp.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class for all chat messages.
 * Uses Jackson polymorphic serialization for different message types.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextMessage.class, name = "text"),
    @JsonSubTypes.Type(value = LoginRequest.class, name = "login"),
    @JsonSubTypes.Type(value = LoginResponse.class, name = "login_response"),
    @JsonSubTypes.Type(value = UserListMessage.class, name = "user_list"),
    @JsonSubTypes.Type(value = SystemMessage.class, name = "system")
})
public abstract class ChatMessage {

    private long timestamp;

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the message type identifier
     */
    public abstract String getMessageType();
}