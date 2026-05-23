package com.chatapp.server.service;

import com.chatapp.server.model.FileMetadata;
import com.chatapp.server.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * Saves an uploaded file to disk and records metadata.
     */
    public FileMetadata storeFile(MultipartFile file, String sender, String recipient) throws IOException {
        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unknown_file";
        }

        // Generate unique stored name
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String fileId = UUID.randomUUID().toString();
        String storedFilename = fileId + extension;

        Path targetLocation = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetLocation);

        // Save metadata
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(fileId);
        metadata.setOriginalFilename(originalFilename);
        metadata.setStoredFilename(storedFilename);
        metadata.setContentType(file.getContentType());
        metadata.setSize(file.getSize());
        metadata.setSender(sender);
        metadata.setRecipient(recipient);
        metadata.setTimestamp(System.currentTimeMillis());

        FileMetadata saved = fileRepository.save(metadata);

        logger.info("Stored file: {} ({} bytes) uploaded by {}", originalFilename, file.getSize(), sender);
        return saved;
    }

    /**
     * Returns the file metadata for a given fileId.
     */
    public FileMetadata getFileMetadata(String fileId) {
        return fileRepository.findByFileId(fileId).orElse(null);
    }

    /**
     * Returns recent files the user has access to (group files or P2P files involving the user).
     */
    public List<FileMetadata> getRecentFilesForUser(String username, long sinceTimestamp) {
        // For simplicity, return all files newer than timestamp.
        // In production you would filter more carefully.
        return fileRepository.findAll().stream()
                .filter(f -> f.getTimestamp() > sinceTimestamp)
                .filter(f -> f.getRecipient() == null
                        || f.getSender().equals(username)
                        || (f.getRecipient() != null && f.getRecipient().equals(username)))
                .sorted((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                .toList();
    }

    /**
     * Returns the actual file path on disk.
     */
    public Path getFilePath(String storedFilename) {
        return Paths.get(uploadDir).resolve(storedFilename);
    }

    /**
     * Scheduled cleanup of old files (runs every hour, same as message cleanup).
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void cleanupOldFiles() {
        // Keep files for 7 days (same as messages)
        long retentionMs = 7L * 24 * 60 * 60 * 1000;
        long cutoff = System.currentTimeMillis() - retentionMs;

        List<FileMetadata> oldFiles = fileRepository.findByTimestampLessThan(cutoff);

        for (FileMetadata meta : oldFiles) {
            try {
                Path filePath = getFilePath(meta.getStoredFilename());
                Files.deleteIfExists(filePath);
                logger.info("Deleted old file from disk: {}", meta.getOriginalFilename());
            } catch (IOException e) {
                logger.warn("Failed to delete file {}: {}", meta.getStoredFilename(), e.getMessage());
            }
        }

        if (!oldFiles.isEmpty()) {
            fileRepository.deleteByTimestampLessThan(cutoff);
            logger.info("Cleaned up {} old file records", oldFiles.size());
        }
    }
}
