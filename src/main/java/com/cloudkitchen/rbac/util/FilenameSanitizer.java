package com.cloudkitchen.rbac.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.cloudkitchen.rbac.constants.AppConstants;

/**
 * Utility class for sanitizing and generating safe filenames for S3 storage.
 * Ensures consistent filename handling across all file upload services.
 */
public final class FilenameSanitizer {
    
    private FilenameSanitizer() {
        // Utility class
    }
    
    /**
     * Sanitizes filename to ensure consistent, safe S3 keys.
     * - Converts to lowercase
     * - Replaces spaces with underscores
     * - Removes invalid characters
     * - Prevents double underscores
     * - Preserves file extension
     * - Adds timestamp and UUID for uniqueness
     * 
     * @param filename Original filename
     * @return Sanitized filename with timestamp and UUID
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return generateUniqueFilename(".jpg");
        }

        String extension = getFileExtension(filename);
        String nameWithoutExt = filename.substring(0,
                filename.lastIndexOf('.') > 0 ? filename.lastIndexOf('.') : filename.length());

        // Sanitize: lowercase, replace spaces, remove invalid chars
        String sanitized = nameWithoutExt.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_-]", "")
                .replaceAll("_{2,}", "_");
        
        // Remove leading/trailing underscores with explicit grouping
        sanitized = sanitized.replaceAll("^(_+)|(_+)$", "");

        // If sanitization removed everything, generate new name
        if (sanitized.isBlank()) {
            return generateUniqueFilename(extension);
        }

        // Add timestamp and UUID for uniqueness
        return generateUniqueFilename(sanitized + extension);
    }
    
    /**
     * Generates a unique filename with timestamp and UUID.
     * 
     * @param filename Base filename (with or without extension)
     * @return Unique filename with timestamp and UUID
     */
    public static String generateUniqueFilename(String filename) {
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern(AppConstants.FileNaming.TIMESTAMP_FORMAT));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(filename);
        String nameWithoutExt = filename.replace(extension, "");

        if (nameWithoutExt.isBlank()) {
            return timestamp + AppConstants.FileNaming.SEPARATOR + uuid + extension;
        }

        return nameWithoutExt + AppConstants.FileNaming.SEPARATOR + timestamp 
            + AppConstants.FileNaming.SEPARATOR + uuid + extension;
    }
    
    /**
     * Gets file extension from filename.
     * 
     * @param filename Filename
     * @return File extension (e.g., ".jpg") or ".jpg" as default
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
    
    /**
     * Sanitizes S3 path to prevent path traversal and invalid characters.
     * 
     * @param path S3 path
     * @return Sanitized path
     * @throws SecurityException if path traversal detected
     */
    public static String sanitizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        
        if (path.contains("..")) {
            throw new SecurityException("Path traversal detected in S3 path");
        }
        
        return path.replaceAll("[^a-zA-Z0-9/_.-]", "");
    }
}

