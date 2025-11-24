package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Batch presigned URL response view object
 * 
 * Response containing presigned URLs for batch file uploads.
 * Each entry contains the original file name and its corresponding presigned URL.
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class BatchPresignedUrlVO implements Serializable {

    /**
     * List of presigned URL entries
     * Each entry contains the original file name and its presigned URL
     */
    private List<PresignedUrlEntry> entries;

    /**
     * Inner class representing a single presigned URL entry
     */
    @Data
    public static class PresignedUrlEntry implements Serializable {
        /**
         * Original file name (before sanitization)
         */
        private String fileName;

        /**
         * Sanitized file name (used in S3)
         */
        private String sanitizedFileName;

        /**
         * Presigned URL for PUT upload
         */
        private String presignedUrl;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}

