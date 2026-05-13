package com.chatapp.server.service;

import com.chatapp.server.model.Message;
import com.chatapp.server.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing chat messages.
 * Handles saving messages and cleanup of old messages.
 */
@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    // Keep messages for 1 day (24 hours * 60 minutes * 60 seconds * 1000 milliseconds)
    private static final long MESSAGE_RETENTION_PERIOD = 24 * 60 * 60 * 1000L;

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Save a chat message to the database.
     *
     * @param sender     The sender username
     * @param content    The message content
     * @param recipient  The recipient username (null for group messages)
     * @param timestamp  The message timestamp
     * @param messageType The type of message (e.g., "text", "system")
     * @return The saved message
     */
    public Message saveMessage(String sender, String content, String recipient, Long timestamp, String messageType) {
        Message message = new Message(sender, content, recipient, timestamp, messageType);
        Message savedMessage = messageRepository.save(message);
        logger.debug("Saved message from {} to {}: {}", sender, recipient != null ? recipient : "ALL", content);
        return savedMessage;
    }

    /**
     * Save a text message to the database.
     *
     * @param sender    The sender username
     * @param content   The message content
     * @param recipient The recipient username (null for group messages)
     * @return The saved message
     */
    public Message saveTextMessage(String sender, String content, String recipient) {
        return saveMessage(sender, content, recipient, System.currentTimeMillis(), "text");
    }

    /**
     * Get chat history for a user (both group and P2P messages) for the last 24 hours.
     *
     * @param username The username to get history for
     * @return List of messages
     */
    public List<Message> getChatHistory(String username) {
        long sinceTimestamp = System.currentTimeMillis() - MESSAGE_RETENTION_PERIOD;
        return messageRepository.findAllMessagesForUser(username, sinceTimestamp);
    }

    /**
     * Get all group messages for the last 24 hours.
     *
     * @return List of group messages
     */
    public List<Message> getGroupMessages() {
        long sinceTimestamp = System.currentTimeMillis() - MESSAGE_RETENTION_PERIOD;
        return messageRepository.findByRecipientIsNullAndTimestampGreaterThanOrderByTimestampAsc(sinceTimestamp);
    }

    /**
     * Get P2P messages for a user (both sent and received) for the last 24 hours.
     *
     * @param username The username to get messages for
     * @return List of P2P messages
     */
    public List<Message> getP2PMessages(String username) {
        long sinceTimestamp = System.currentTimeMillis() - MESSAGE_RETENTION_PERIOD;
        List<Message> received = messageRepository.findReceivedMessages(username, sinceTimestamp);
        List<Message> sent = messageRepository.findSentMessages(username, sinceTimestamp);

        // Merge and sort by timestamp
        received.addAll(sent);
        received.sort((m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
        return received;
    }

    /**
     * Scheduled task to delete messages older than 1 day.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // Every 1 hour
    public void cleanupOldMessages() {
        long cutoffTimestamp = System.currentTimeMillis() - MESSAGE_RETENTION_PERIOD;
        long deletedCount = messageRepository.deleteByTimestampLessThan(cutoffTimestamp);
        if (deletedCount > 0) {
            logger.info("Cleaned up {} old messages older than 24 hours", deletedCount);
        }
    }

    /**
     * Get the message retention period in milliseconds.
     */
    public long getMessageRetentionPeriod() {
        return MESSAGE_RETENTION_PERIOD;
    }
}