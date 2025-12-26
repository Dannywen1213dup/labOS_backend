package com.labOS.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * File Upload Record Entity
 * Tracks individual file upload operations with detailed metadata
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@TableName(value = "file_upload_record")
@Data
public class FileUploadRecord implements Serializable {

    /**
     * Primary key
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Associated batch ID (FK to file_upload_batch)
     */
    private String batchId;

    /**
     * User ID who initiated the upload
     */
    private Long userId;

    /**
     * Original file name provided by user
     */
    private String originalFileName;

    /**
     * Sanitized file name (safe for S3)
     */
    private String sanitizedFileName;

    /**
     * Full S3 object key (path + filename)
     */
    private String s3Key;

    /**
     * S3 bucket name
     */
    private String s3Bucket;

    /**
     * File size in bytes (if known)
     */
    private Long fileSize;

    /**
     * MIME content type
     */
    private String contentType;

    /**
     * Generated presigned URL (for reference)
     */
    private String presignedUrl;

    /**
     * Upload status: PENDING, SUCCESS, FAILED, EXPIRED
     */
    private String uploadStatus;

    /**
     * Upload method: PRESIGNED_URL, DIRECT, etc.
     */
    private String uploadMethod;

    /**
     * Error message if upload failed
     */
    private String errorMessage;

    /**
     * Error code if upload failed
     */
    private String errorCode;

    /**
     * HTTP status code from S3 response
     */
    private Integer httpStatusCode;

    /**
     * Number of retry attempts
     */
    private Integer retryCount;

    /**
     * Presigned URL expiration time
     */
    private Date urlExpirationTime;

    /**
     * Presigned URL request time (UTC)
     */
    private Date requestTime;

    /**
     * Upload start time (UTC)
     */
    private Date uploadStartTime;

    /**
     * Upload completion time (UTC)
     */
    private Date uploadCompletionTime;

    /**
     * Last update time (UTC)
     */
    private Date updateTime;

    /**
     * Client timezone
     */
    private String clientTimezone;

    /**
     * Additional metadata (JSON format)
     */
    private String metadata;

    /**
     * Is deleted
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

