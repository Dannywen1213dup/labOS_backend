package com.labOS.backend.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * File Upload Status Update Request
 * Used by frontend to report upload success/failure
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class FileUploadStatusUpdateRequest implements Serializable {

    /**
     * Batch ID
     */
    private String batchId;

    /**
     * Sanitized file name (to identify which file in the batch)
     */
    private String sanitizedFileName;

    /**
     * Upload status: SUCCESS, FAILED
     */
    private String uploadStatus;

    /**
     * File size in bytes (optional, for successful uploads)
     */
    private Long fileSize;

    /**
     * Error message (for failed uploads)
     */
    private String errorMessage;

    /**
     * Error code (for failed uploads)
     */
    private String errorCode;

    /**
     * HTTP status code from S3
     */
    private Integer httpStatusCode;

    /**
     * Client timezone
     */
    private String clientTimezone;

    private static final long serialVersionUID = 1L;
}

