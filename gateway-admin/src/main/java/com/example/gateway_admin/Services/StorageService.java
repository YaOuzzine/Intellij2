// gateway-admin/src/main/java/com/example/gateway_admin/Services/StorageService.java
package com.example.gateway_admin.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    @Value("${storage.location:uploads}")
    private String storageLocation;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    /**
     * Initialize storage directory
     */
    public void init() {
        try {
            Path uploadPath = Paths.get(storageLocation);
            Path profilesPath = uploadPath.resolve("profiles");

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created uploads directory: {}", uploadPath.toAbsolutePath());
            }

            if (!Files.exists(profilesPath)) {
                Files.createDirectories(profilesPath);
                logger.info("Created profiles directory: {}", profilesPath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Could not initialize storage locations", e);
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    /**
     * Store a profile image and return its URL
     */
    public String storeProfileImage(MultipartFile file, String username) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Failed to store empty file");
            }

            // Initialize storage if needed
            init();

            // Generate a unique filename to prevent collisions
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String newFilename = username + "_" + UUID.randomUUID() + fileExtension;

            // Create the profiles directory path
            Path profilesDir = Paths.get(storageLocation, "profiles");

            // Create the target file path
            Path destinationFile = profilesDir.resolve(newFilename).normalize().toAbsolutePath();

            // Ensure the file is within the profiles directory (security check)
            if (!destinationFile.getParent().equals(profilesDir.toAbsolutePath())) {
                throw new SecurityException("Storage destination is outside of the allowed directory");
            }

            // Copy the file to the destination
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // Return the URL path to the stored file
            String imageUrl = baseUrl + "/api/uploads/profiles/" + newFilename;
            logger.info("Stored profile image for user {} at {}", username, imageUrl);

            return imageUrl;

        } catch (IOException e) {
            logger.error("Failed to store file", e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * Extract file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return ".jpg"; // Default extension
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}