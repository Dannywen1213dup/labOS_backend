package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * File Upload Record View Object
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class FileUploadRecordVO implements Serializable {

    /**
     * Record ID
     */
    private Long id;

    /**
     * Original file name
     */
    private String originalFileName;

    /**
     * Sanitized file name
     */
    private String sanitizedFileName;

    /**
     * S3 key
     */
    private String s3Key;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * File size formatted (e.g., "1.5 MB")
     */
    private String fileSizeFormatted;

    /**
     * Content type
     */
    private String contentType;

    /**
     * Upload status
     */
    private String uploadStatus;

    /**
     * Error message
     */
    private String errorMessage;

    /**
     * Request time
     */
    private Date requestTime;

    /**
     * Upload completion time
     */
    private Date uploadCompletionTime;

    /**
     * Duration in seconds
     */
    private Long durationSeconds;

    /**
     * Retry count
     */
    private Integer retryCount;

    private static final long serialVersionUID = 1L;
}

