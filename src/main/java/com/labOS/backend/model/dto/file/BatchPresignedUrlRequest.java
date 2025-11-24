package com.labOS.backend.model.dto.file;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Batch generate presigned URLs for upload request
 * 
 * Request DTO for generating multiple presigned URLs that allow direct upload to S3.
 * Used for batch file uploads where multiple files need to be uploaded at once.
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class BatchPresignedUrlRequest implements Serializable {

    /**
     * List of file names to upload (required)
     * 
     * Each file name will be sanitized to ensure S3 bucket safety.
     * Special characters will be removed/replaced with safe alternatives.
     */
    private List<String> fileNames;

    /**
     * Expiration time in milliseconds (optional)
     * 
     * The time in milliseconds after which the presigned URLs will expire.
     * Default: 3600000 (1 hour)
     */
    private Long expirationTime;

    private static final long serialVersionUID = 1L;
}

