# Chat Application Specification

## 1. Project Overview

### Project Name
Chat Application

### Project Type
Real-time messaging application with client-server architecture

### Core Functionality
A multi-user chat application where users can connect via a JavaFX GUI client and exchange text messages and files (group + P2P) in real-time through a Spring Boot WebSocket + HTTP server.

### Target Users
- Desktop users on Windows, macOS, or Linux
- Administrators managing users via web interface

---

## 2. Technical Stack

### Server Technology
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.2.5 |
| WebSocket | Spring WebSocket + STOMP | - |
| Database | SQLite | 3.45.1 |
| ORM | Hibernate | 6.4.4 |
| Security | Spring Security | 6.2.4 |
| Template Engine | Thymeleaf | - |

### Client Technology
| Component | Technology | Version |
|-----------|------------|---------|
| GUI Framework | JavaFX | 21.0.2 |
| WebSocket Client | Spring STOMP | 6.1.6 |
| JSON | Jackson | - |
| Build Tool | Maven | - |
| Theme | Modern Dark UI (Discord/Telegram-inspired) | - |

### Common Library
| Component | Technology |
|-----------|------------|
| Message Models | Jackson JSON |
| Build Tool | Maven |

---

## 3. Module Specification

### 3.1 chat-common Module

**Purpose:** Shared message models and serialization utilities

**Package:** `com.chatapp.common`

**Classes:**
- `ChatMessage` - Abstract base class with timestamp
- `TextMessage` - Chat message with sender, content, recipient
- `FileMessage` - File sharing metadata (fileId, filename, size, contentType)
- `LoginRequest` - Client login credentials
- `LoginResponse` - Server login response (success/failure)
- `UserListMessage` - List of connected users (with online status)
- `SystemMessage` - System notifications (user joined/left)
- `MessageSerializer` - Jackson JSON serialization utility

**Message Format (JSON):**
```json
{
  "type": "text|file|login|login_response|user_list|system",
  "timestamp": 1234567890,
  "sender": "username",
  "content": "message text",
  "recipient": "target_username",
  "fileId": "uuid-for-download",
  "originalFilename": "report.pdf",
  "size": 245760,
  "contentType": "application/pdf"
}
```

### 3.2 chat-server Module

**Purpose:** Spring Boot WebSocket server with user management

**Package:** `com.chatapp.server`

**Components:**

#### Configuration
- `SqliteConfig` - SQLite database configuration
- `SecurityConfig` - Web security with form login
- `WebSocketConfig` - STOMP WebSocket endpoint configuration

#### REST Controllers
- `UserManagementController` - Thymeleaf web pages
  - GET `/` - Redirect to /users
  - GET `/users` - List all users
  - GET `/users/new` - Create user form
  - POST `/users` - Create new user

- `UserApiController` - REST API
  - GET `/api/users` - List active users
  - POST `/api/users` - Create user
  - POST `/api/users/{username}/activate` - Activate user
  - POST `/api/users/{username}/deactivate` - Deactivate user

#### WebSocket Handler
- `ChatWebSocketHandler` - STOMP message handling
  - `@MessageMapping("/chat")` - Handle incoming messages
  - Broadcast text messages to `/topic/messages`
  - Send login responses to `/topic/login`
  - Broadcast user list to `/topic/users`

#### Database
- `User` JPA Entity - id, username, password, active, createdAt
- `UserRepository` - Spring Data JPA repository
- `UserService` - Business logic with BCrypt password encoding

#### Message Storage
- `Message` JPA Entity - id, sender, content, recipient, timestamp, messageType
- `MessageRepository` - Spring Data JPA repository for message queries
- `MessageService` - Business logic for saving messages and cleanup

#### REST API for Chat
- `ChatApiController` - REST endpoints for chat history
  - GET `/api/chat/history/{username}` - Get all messages for user (last 24h)
  - GET `/api/chat/group` - Get group/broadcast messages (last 24h)
  - GET `/api/chat/p2p/{username}` - Get P2P messages for user (last 24h)
  - GET `/api/chat/retention` - Get message retention period

**WebSocket Endpoints:**
| Endpoint | Purpose |
|----------|---------|
| `/ws` | WebSocket connection endpoint |
| `/app/chat` | Application destination for messages |
| `/topic/messages` | Broadcast messages |
| `/topic/login` | Login responses |
| `/topic/users` | User list updates |
| `/user/queue/messages` | Personal queue for P2P messages |
| `/topic/user/{username}/messages` | P2P message delivery |
| `/topic/user/{username}/history` | Chat history on login (last 24h) |

**P2P Message Flow:**
```
1. Client connects with username header in STOMP CONNECT frame
2. Server StompChannelInterceptor sets user Principal from username header
3. User selects peer from sidebar
4. Client switches to P2P chat mode
5. User types message and sends
6. Client sends TextMessage with recipient field set
7. Server routes to /user/{recipient}/queue/messages
8. Recipient receives message on their personal queue
9. Recipient sees unread count in sidebar if not viewing that chat
```

**STOMP Headers for P2P:**
- CONNECT frame must include `username` header
- P2P messages use `/user/{username}/queue/messages` destination
- `convertAndSendToUser()` requires user Principal to be set

**Database Schema:**
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    sender VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    recipient VARCHAR(50),
    timestamp BIGINT NOT NULL,
    message_type VARCHAR(20)
);
```

### 3.3 chat-client Module

**Purpose:** JavaFX desktop GUI client with modern dark theme UI

**Package:** `com.chatapp.client`

**Components:**

#### Application
- `ChatClientApplication` - Main JavaFX application
  - showLoginView() - Display login screen
  - showChatView() - Display chat screen

#### Controllers
- `LoginController` - Login form handling
  - Server URL input
  - Username/password fields
  - Connect button

- `ChatController` - Chat view handling
  - Message display list
  - User list sidebar
  - Message input field
  - Send/disconnect buttons

#### Network
- `ChatStompClient` - STOMP WebSocket client
  - Connect to WebSocket server
  - Subscribe to topics
  - Send/receive messages
  - Handle connection errors

#### Models
- `MessageItem` - JavaFX model for message display

**UI Screens:**

1. **Login Screen**
   - Server URL field (default: ws://localhost:8080/ws)
   - Username field
   - Password field
   - Login button
   - Size: 400x300 pixels

2. **Chat Screen**
   - Top: Chat target label (shows "Group Chat" or "Chat with: [username]")
   - Middle: Message area with scrollable message bubbles
   - Right panel: User list sidebar with click-to-chat hint
   - Bottom: Message input + Send button
   - "Back" button appears when in P2P mode
   - Size: 800x600 pixels (minimum: 600x400)

**Message Bubble Design:**
- **Own messages**: Blue background, right-aligned using `BorderPane.setRight()`, sender name, content, timestamp
- **Other messages**: White background with gray border, left-aligned using `BorderPane.setLeft()`, sender name, content, timestamp
- **System messages**: Italic gray text, centered, no bubble

---

## 4. Feature Specification

### 4.1 User Management (Server)

| Feature | Description |
|---------|-------------|
| Create User | Add new user via web form |
| List Users | View all active users |
| Password Storage | BCrypt hashed passwords |
| User Activation | Enable/disable user accounts |

### 4.2 Authentication (Server/Client)

| Feature | Description |
|---------|-------------|
| Web Login | Form-based login for web UI |
| Client Login | Username/password via WebSocket |
| Session Management | HTTP session for web, WebSocket session for client |

### 4.3 Messaging (Client/Server)

| Feature | Description |
|---------|-------------|
| Connect | Establish WebSocket connection |
| Login | Authenticate with server |
| Send Message | Send text or files (group or P2P) |
| Receive Message | Display text and file cards with timestamps |
| Timestamps | Messages show date + time (e.g. "May 23, 20:42") |
| User List | Show connected users with online/offline status dots |
| System Notifications | User join/leave messages (persisted in history) |
| Disconnect | Clean connection shutdown |

### 4.4 Peer-to-Peer Chat (Client/Server)

| Feature | Description |
|---------|-------------|
| Select Peer | Click on user in sidebar to start P2P chat |
| P2P Messaging | Send private messages to specific user |
| P2P Indicator | Header shows "Group Chat" or "Chat with: [username]" |
| Unread Count | User list shows unread count in red bold (e.g., "user (3)") |
| Online Status | Visual indicator (green dot = online, gray dot = offline) next to each peer |
| Message Storage | P2P messages stored per conversation |
| Back to Group | Button to return to broadcast chat |
| Auto-switch | Incoming P2P messages shown if currently viewing that chat |

### 4.5 Chat Persistence

| Feature | Description |
|---------|-------------|
| Message Storage | All messages (group and P2P) + file metadata stored in SQLite |
| Retention Period | Messages and files retained for 1 week |
| Auto Cleanup | Scheduled task deletes messages and files older than 1 week |
| Chat History | Users receive chat history (text + files) on login via WebSocket |
| REST API | Programmatic access to chat history and file download |

### 4.6 Real-time Features

| Feature | Description |
|---------|-------------|
| Live Updates | Instant message delivery |
| Multi-user | Multiple clients connected simultaneously |
| Auto-refresh | User list updates on connect/disconnect |
| Online Status | Real-time green/gray dots indicating peer availability |

### 4.7 File Sharing

| Feature | Description |
|---------|-------------|
| File Upload | Attach and upload files (up to 50MB) via HTTP multipart |
| Group & P2P Support | Files can be shared in both group chat and private 1:1 chats |
| File Announcement | Lightweight `FileMessage` (metadata only) broadcast over WebSocket |
| File Cards | Modern UI cards showing filename, size, sender, and Download button |
| Upload Progress | Real-time progress bar + percentage during upload |
| Error Handling | Clear feedback for size limits, network errors, etc. |
| Secure Download | Files retrieved via HTTP `/api/files/download/{fileId}` |
| Persistence | File metadata stored in database; files retained for 1 week |
| History | Previously shared files appear in chat history on login |

---

## 5. Data Flow

### Login Flow
```
1. Client connects to /ws
2. Client subscribes to /topic/login
3. Client sends LoginRequest to /app/chat
4. Server validates credentials
5. Server sends LoginResponse to /topic/login
6. Client receives response and transitions to chat view
```

### Message Flow
```
1. User enters message in input field
2. Client sends TextMessage to /app/chat
3. Server saves message to SQLite database
4. Server broadcasts to /topic/messages
5. All connected clients receive message
6. Each client displays message in message list
```

### Chat History Flow
```
1. User logs in successfully
2. Server retrieves messages from last 24 hours for user
3. Server sends history messages to /topic/user/{username}/history
4. Client displays historical messages in chat view
```

---

## 6. Configuration

### Server Configuration (application.yml)

```yaml
server:
  port: 8080
  address: 0.0.0.0

spring:
  datasource:
    url: jdbc:sqlite:chat-server.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.community.dialect.SQLiteDialect
  thymeleaf:
    cache: false
```

### Client Configuration

- Default Server URL: `ws://localhost:8080/ws`
- Configurable at runtime via login form

---

## 7. Build Configuration

### Java Version
- Source: Java 21
- Target: Java 21

### Parent POM Properties
| Property | Value |
|----------|-------|
| java.version | 21 |
| spring-boot.version | 3.2.5 |
| javafx.version | 21.0.2 |
| sqlite-jdbc.version | 3.45.1.0 |

### Modules
1. chat-common - 1.0.0-SNAPSHOT
2. chat-server - 1.0.0-SNAPSHOT (depends on chat-common)
3. chat-client - 1.0.0-SNAPSHOT (depends on chat-common)

---

## 8. Acceptance Criteria

### Server
- [ ] Server starts without errors on port 8080
- [ ] Web UI accessible at /users
- [ ] Can create new users via web form
- [ ] WebSocket endpoint available at /ws
- [ ] STOMP messaging working
- [ ] SQLite database created automatically
- [ ] Messages stored in SQLite database
- [ ] Chat history sent to user on login
- [ ] Old messages and files automatically deleted after 1 week

### Client
- [ ] Login screen displays on launch
- [ ] Can connect to server with valid credentials
- [ ] Chat view displays after successful login (modern dark theme)
- [ ] Messages sent appear in chat with date + time
- [ ] Messages from other users appear in real-time
- [ ] User list shows online/offline status (green/gray dots)
- [ ] File sharing works in group and P2P chats
- [ ] Upload progress is shown during file uploads
- [ ] File cards with download buttons are displayed
- [ ] Disconnect button works
- [ ] Chat history (text + files) loads on login

### Integration
- [ ] Multiple clients can connect simultaneously
- [ ] Messages and files broadcast to relevant users (group or P2P)
- [ ] User join/leave notifications work and persist in history
- [ ] P2P messages and files stored and retrievable

---

## 9. File Structure

```
chat-app/
├── pom.xml
├── README.md
├── SPEC.md
├── .gitignore
├── chat-common/
│   ├── pom.xml
│   └── src/main/java/com/chatapp/common/
│       ├── MessageSerializer.java
│       └── model/
│           ├── ChatMessage.java
│           ├── TextMessage.java
│           ├── LoginRequest.java
│           ├── LoginResponse.java
│           ├── UserListMessage.java
│           └── SystemMessage.java
├── chat-server/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/chatapp/server/
│       │   ├── ChatServerApplication.java
│       │   ├── config/
│       │   │   ├── SqliteConfig.java
│       │   │   ├── SecurityConfig.java
│       │   │   └── WebSocketConfig.java
│       │   ├── controller/
│       │   │   ├── UserApiController.java
│       │   │   ├── UserManagementController.java
│       │   │   └── ChatApiController.java
│       │   ├── model/
│       │   │   ├── User.java
│       │   │   └── Message.java
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   └── MessageRepository.java
│       │   ├── service/
│       │   │   ├── UserService.java
│       │   │   └── MessageService.java
│       │   └── websocket/
│       │       └── ChatWebSocketHandler.java
│       └── resources/
│           ├── application.yml
│           └── templates/
│               ├── users.html
│               └── user-form.html
└── chat-client/
    ├── pom.xml
    └── src/main/
        ├── java/com/chatapp/client/
        │   ├── ChatClientApplication.java
        │   ├── controller/
        │   │   ├── ChatController.java
        │   │   └── LoginController.java
        │   ├── model/
        │   │   └── MessageItem.java
        │   └── network/
        │       ├── ChatStompClient.java
        │       └── ChatWebSocketClient.java
        └── resources/
            ├── css/styles.css
            └── fxml/
                ├── chat.fxml
                └── login.fxml
```