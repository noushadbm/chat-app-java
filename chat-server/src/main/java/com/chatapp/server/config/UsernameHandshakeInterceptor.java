package com.chatapp.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Interceptor to extract username from WebSocket handshake and set as attribute.
 */
public class UsernameHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UsernameHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String username = servletRequest.getServletRequest().getParameter("username");
            if (username != null && !username.isEmpty()) {
                attributes.put("username", username);
                logger.info("Handshake: Extracted username: {}", username);
            } else {
                // Username will be extracted from login message
                logger.info("Handshake: No username in URL, will extract from login message");
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after handshake
    }
}