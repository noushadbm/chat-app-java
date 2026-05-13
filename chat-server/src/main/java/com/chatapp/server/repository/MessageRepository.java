package com.chatapp.server.repository;

import com.chatapp.server.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity operations.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find all group messages (no recipient) within the last specified timestamp.
     *
     * @param sinceTimestamp The timestamp to filter messages from
     * @return List of group messages
     */
    List<Message> findByRecipientIsNullAndTimestampGreaterThanOrderByTimestampAsc(
            @Param("sinceTimestamp") Long sinceTimestamp);

    /**
     * Find all P2P messages where user is either sender or recipient within the last specified timestamp.
     *
     * @param username The username to filter messages for
     * @param sinceTimestamp The timestamp to filter messages from
     * @return List of P2P messages
     */
    @Query("SELECT m FROM Message m WHERE m.recipient = :username AND m.timestamp > :sinceTimestamp ORDER BY m.timestamp ASC")
    List<Message> findReceivedMessages(@Param("username") String username,
                                        @Param("sinceTimestamp") Long sinceTimestamp);

    /**
     * Find all P2P messages sent by a user within the last specified timestamp.
     *
     * @param username The username to filter messages for
     * @param sinceTimestamp The timestamp to filter messages from
     * @return List of P2P messages sent by user
     */
    @Query("SELECT m FROM Message m WHERE m.sender = :username AND m.recipient IS NOT NULL AND m.timestamp > :sinceTimestamp ORDER BY m.timestamp ASC")
    List<Message> findSentMessages(@Param("username") String username,
                                    @Param("sinceTimestamp") Long sinceTimestamp);

    /**
     * Find all messages (both group and P2P) for a user within the last specified timestamp.
     * This includes group messages and P2P messages where user is sender or recipient.
     *
     * @param username The username to filter messages for
     * @param sinceTimestamp The timestamp to filter messages from
     * @return List of all messages for the user
     */
    @Query("SELECT m FROM Message m WHERE (m.recipient IS NULL OR m.sender = :username OR m.recipient = :username) " +
           "AND m.timestamp > :sinceTimestamp ORDER BY m.timestamp ASC")
    List<Message> findAllMessagesForUser(@Param("username") String username,
                                         @Param("sinceTimestamp") Long sinceTimestamp);

    /**
     * Find all messages older than the specified timestamp.
     *
     * @param timestamp The timestamp to filter messages older than
     * @return List of old messages to be deleted
     */
    List<Message> findByTimestampLessThan(Long timestamp);

    /**
     * Delete all messages older than the specified timestamp.
     *
     * @param timestamp The timestamp to filter messages older than
     * @return Number of deleted messages
     */
    long deleteByTimestampLessThan(Long timestamp);
}