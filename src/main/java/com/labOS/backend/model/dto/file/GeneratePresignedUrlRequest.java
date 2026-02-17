package com.labOS.backend.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * Generate presigned URL for upload request
 * 
 * Request DTO for generating a presigned URL that allows direct upload to S3.
 * The presigned URL can be used by clients to upload files directly to S3 without
 * going through the backend server.
 * 
 * Files are automatically stored in a folder structure based on the logged-in user's ID:
 * labOS/{userId}/{MMDDYYYY}/{count}/{sanitizedFileName}
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class GeneratePresignedUrlRequest implements Serializable {

    /**
     * File name to upload (required)
     * 
     * The name of the file that will be uploaded to S3.
     * Special characters will be sanitized to ensure S3 bucket safety.
     * The filename will be cleaned to remove/replace unsafe characters.
     * 
     * Example: "document.pdf", "my file (1).jpg" -> "my_file_1_.jpg"
     */
    private String fileName;

    /**
     * Expiration time in milliseconds (optional)
     * 
     * The time in milliseconds after which the presigned URL will expire.
     * Default: 3600000 (1 hour)
     * 
     * Example values:
     * - 3600000 = 1 hour
     * - 7200000 = 2 hours
     * - 1800000 = 30 minutes
     */
    private Long expirationTime;

    private static final long serialVersionUID = 1L;
}

