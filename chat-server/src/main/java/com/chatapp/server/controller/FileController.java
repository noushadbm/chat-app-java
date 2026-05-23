package com.chatapp.server.controller;

import com.chatapp.server.model.FileMetadata;
import com.chatapp.server.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Upload a file. Returns metadata that the client can use to announce the file in chat.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "username", required = false) String username,
            @RequestParam(value = "recipient", required = false) String recipient) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        // Simple size limit (50 MB)
        if (file.getSize() > 50 * 1024 * 1024) {
            return ResponseEntity.badRequest().body("File too large (max 50MB)");
        }

        try {
            FileMetadata metadata = fileService.storeFile(file, username, recipient);

            return ResponseEntity.ok(new FileUploadResponse(
                    metadata.getFileId(),
                    metadata.getOriginalFilename(),
                    metadata.getSize(),
                    metadata.getContentType()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Download a file by its fileId.
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        try {
            FileMetadata metadata = fileService.getFileMetadata(fileId);
            if (metadata == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = fileService.getFilePath(metadata.getStoredFilename());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            String contentType = metadata.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Simple DTO for upload response
    public static class FileUploadResponse {
        public String fileId;
        public String filename;
        public long size;
        public String contentType;

        public FileUploadResponse(String fileId, String filename, long size, String contentType) {
            this.fileId = fileId;
            this.filename = filename;
            this.size = size;
            this.contentType = contentType;
        }
    }

    /**
     * Gracefully handle files that exceed the configured upload limit.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(413) // Payload Too Large
                .body("File too large. Maximum allowed size is 50MB.");
    }
}
