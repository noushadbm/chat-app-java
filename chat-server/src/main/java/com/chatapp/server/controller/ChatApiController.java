package com.chatapp.server.controller;

import com.chatapp.server.model.Message;
import com.chatapp.server.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for chat messages.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatApiController {

    private final MessageService messageService;

    public ChatApiController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Get chat history for a specific user.
     * Returns both group messages and P2P messages for the last 24 hours.
     *
     * @param username The username to get history for
     * @return List of messages
     */
    @GetMapping("/history/{username}")
    public ResponseEntity<?> getChatHistory(@PathVariable String username) {
        List<Message> messages = messageService.getChatHistory(username);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get group messages (broadcast) for the last 24 hours.
     *
     * @return List of group messages
     */
    @GetMapping("/group")
    public ResponseEntity<List<Message>> getGroupMessages() {
        List<Message> messages = messageService.getGroupMessages();
        return ResponseEntity.ok(messages);
    }

    /**
     * Get P2P messages for a user for the last 24 hours.
     *
     * @param username The username to get messages for
     * @return List of P2P messages
     */
    @GetMapping("/p2p/{username}")
    public ResponseEntity<List<Message>> getP2PMessages(@PathVariable String username) {
        List<Message> messages = messageService.getP2PMessages(username);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get message retention period information.
     *
     * @return Retention period in milliseconds
     */
    @GetMapping("/retention")
    public ResponseEntity<Map<String, Long>> getRetentionPeriod() {
        return ResponseEntity.ok(Map.of("retentionMs", messageService.getMessageRetentionPeriod()));
    }
}