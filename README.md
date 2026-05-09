# Chat Application

A multi-module Java chat application with a Spring Boot server and JavaFX client.

## Project Structure

```
chat-app/
├── pom.xml                    # Parent Maven configuration
├── chat-common/               # Shared message models
├── chat-server/               # Spring Boot WebSocket server
└── chat-client/               # JavaFX GUI client
```

## Requirements

- Java 21
- Maven 3.8+
- SQLite (embedded)

## Build

```bash
mvn clean install -DskipTests
```

## Running

### Start the Server

```bash
mvn spring-boot:run -pl chat-server
```

The server starts on `http://localhost:8080`

### Start the Client

```bash
mvn javafx:run -pl chat-client
```

## Usage

### Creating Users

1. Open your browser to `http://localhost:8080/users`
2. Click "Add New User" to create a new user
3. Enter username and password

### Chatting

1. Run the JavaFX client
2. Enter server URL: `ws://localhost:8080/ws`
3. Enter username and password (created via web UI)
4. Click Login to connect to the chat

Messages are broadcast to all connected users.

## Architecture

### Server
- Spring Boot 3.2.5 with WebSocket (STOMP) support
- SQLite database for user persistence
- Spring Security for web UI authentication
- Thymeleaf for server-side rendering

### Client
- JavaFX 21 for cross-platform GUI
- STOMP WebSocket client for real-time messaging
- MVC architecture with FXML views

### Communication
- WebSocket with STOMP protocol
- JSON message format using Jackson
- Message types: login, text, system, user_list

## Configuration

### Server (application.yml)
- Port: 8080
- Database: SQLite (chat-server.db)
- WebSocket endpoint: /ws

### Client
- Default server URL: ws://localhost:8080/ws
- Configurable at login

## License

MIT