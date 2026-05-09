package com.chatapp.common;

import com.chatapp.common.model.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility class for serializing and deserializing chat messages.
 */
public class MessageSerializer {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * Serialize a ChatMessage to JSON string.
     *
     * @param message The message to serialize
     * @return JSON string representation
     * @throws JsonProcessingException If serialization fails
     */
    public static String serialize(ChatMessage message) throws JsonProcessingException {
        return mapper.writeValueAsString(message);
    }

    /**
     * Deserialize a JSON string to ChatMessage.
     *
     * @param json The JSON string to deserialize
     * @return ChatMessage object
     * @throws JsonProcessingException If deserialization fails
     */
    public static ChatMessage deserialize(String json) throws JsonProcessingException {
        return mapper.readValue(json, ChatMessage.class);
    }

    /**
     * Get the ObjectMapper instance for custom configurations.
     *
     * @return The ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return mapper;
    }
}