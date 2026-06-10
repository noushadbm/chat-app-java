FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom files
COPY pom.xml .
COPY chat-common/pom.xml chat-common/pom.xml
COPY chat-server/pom.xml chat-server/pom.xml
COPY chat-client/pom.xml chat-client/pom.xml

# Copy source code
COPY chat-common/src chat-common/src
COPY chat-server/src chat-server/src
COPY chat-client/src chat-client/src

# Build the project, building chat-common and chat-server
RUN mvn -f pom.xml package -DskipTests -am -pl chat-server

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV CHAT_DB_URL=jdbc:sqlite:/data/chat-server.db
ENV FILE_UPLOAD_DIR=/data/uploads

RUN mkdir -p /data/uploads
VOLUME ["/data"]

# Copy the built jar from the builder stage
COPY --from=builder /app/chat-server/target/chat-server-1.0.0-SNAPSHOT.jar app.jar

# Expose the port the server runs on
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
