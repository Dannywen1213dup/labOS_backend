package com.labOS.backend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * File Upload Batch Entity
 * Tracks batch upload operations for comprehensive monitoring
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@TableName(value = "file_upload_batch")
@Data
public class FileUploadBatch implements Serializable {

    /**
     * Primary key
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Unique batch identifier (UUID)
     */
    private String batchId;

    /**
     * User ID who initiated the upload
     */
    private Long userId;

    /**
     * User email at the time of upload
     */
    private String userEmail;

    /**
     * User name at the time of upload
     */
    private String userName;

    /**
     * User role at the time of upload
     */
    private String userRole;

    /**
     * Folder type: dataset, benchmark-eval, etc.
     */
    private String folderType;

    /**
     * S3 folder path for this batch
     */
    private String folderPath;

    /**
     * Total number of files in this batch
     */
    private Integer totalFiles;

    /**
     * Number of successfully uploaded files
     */
    private Integer successfulFiles;

    /**
     * Number of failed uploads
     */
    private Integer failedFiles;

    /**
     * Number of pending uploads
     */
    private Integer pendingFiles;

    /**
     * Batch status: PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIAL_SUCCESS
     */
    private String batchStatus;

    /**
     * Presigned URL expiration time in milliseconds
     */
    private Long expirationTime;

    /**
     * Client timezone at request time
     */
    private String requestTimezone;

    /**
     * Client IP address
     */
    private String requestIp;

    /**
     * Client user agent
     */
    private String userAgent;

    /**
     * Additional notes or metadata
     */
    private String notes;

    /**
     * Batch creation time (UTC)
     */
    private Date createTime;

    /**
     * Last update time (UTC)
     */
    private Date updateTime;

    /**
     * Batch completion time (UTC)
     */
    private Date completionTime;

    /**
     * Is deleted
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

