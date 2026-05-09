# Chat Application Specification

## 1. Project Overview

### Project Name
Chat Application

### Project Type
Real-time messaging application with client-server architecture

### Core Functionality
A multi-user chat application where users can connect via a JavaFX GUI client and exchange text messages in real-time through a Spring Boot WebSocket server.

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
- `LoginRequest` - Client login credentials
- `LoginResponse` - Server login response (success/failure)
- `UserListMessage` - List of connected users
- `SystemMessage` - System notifications (user joined/left)
- `MessageSerializer` - Jackson JSON serialization utility

**Message Format (JSON):**
```json
{
  "type": "text|login|login_response|user_list|system",
  "timestamp": 1234567890,
  "sender": "username",
  "content": "message text",
  "messageType": "login"
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

**WebSocket Endpoints:**
| Endpoint | Purpose |
|----------|---------|
| `/ws` | WebSocket connection endpoint |
| `/app/chat` | Application destination for messages |
| `/topic/messages` | Broadcast messages |
| `/topic/login` | Login responses |
| `/topic/users` | User list updates |

**Database Schema:**
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.3 chat-client Module

**Purpose:** JavaFX desktop GUI client

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
   - Left panel: Message list (scrollable)
   - Right panel: User list sidebar
   - Bottom: Message input + Send button
   - Size: 800x600 pixels (minimum: 600x400)

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
| Send Message | Broadcast text to all users |
| Receive Message | Display incoming messages |
| User List | Show connected users |
| System Notifications | User join/leave messages |
| Disconnect | Clean connection shutdown |

### 4.4 Real-time Features

| Feature | Description |
|---------|-------------|
| Live Updates | Instant message delivery |
| Multi-user | Multiple clients connected simultaneously |
| Auto-refresh | User list updates on connect/disconnect |

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
3. Server broadcasts to /topic/messages
4. All connected clients receive message
5. Each client displays message in message list
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

### Client
- [ ] Login screen displays on launch
- [ ] Can connect to server with valid credentials
- [ ] Chat view displays after successful login
- [ ] Messages sent appear in chat
- [ ] Messages from other users appear in real-time
- [ ] User list shows connected users
- [ ] Disconnect button works

### Integration
- [ ] Multiple clients can connect simultaneously
- [ ] Messages broadcast to all connected clients
- [ ] User join/leave notifications work

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
│       │   │   └── UserManagementController.java
│       │   ├── model/
│       │   │   └── User.java
│       │   ├── repository/
│       │   │   └── UserRepository.java
│       │   ├── service/
│       │   │   └── UserService.java
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