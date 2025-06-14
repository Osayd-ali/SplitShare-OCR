package com.splitshare.splitshare.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadController {private static final List<String> ALLOWED_TYPES = List.of(
        "image/jpeg", "image/png", "application/pdf"
);
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    @PostMapping("/upload")
    public ResponseEntity<?> uploadReceipt(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file selected."));
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity
                    .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("error", "Unsupported file format. Please upload a valid image or PDF file."));

        }
        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "File too large. Please upload a file smaller than 5MB."));
        }
        try {
            // Create storage directory if it doesn't exist
            Path storagePath = Paths.get("uploads").toAbsolutePath();
            Files.createDirectories(storagePath);

            // Generate UUID file name
            String extension = getExtension(file.getOriginalFilename());
            String newFileName = UUID.randomUUID().toString() + "." + extension;
            Path fullPath = storagePath.resolve(newFileName);

            // Save file
            file.transferTo(fullPath.toFile());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Upload successful");
            response.put("fileName", newFileName);
            response.put("filePath", fullPath.toString());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed due to a server error."));
        }
    }
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }
}
