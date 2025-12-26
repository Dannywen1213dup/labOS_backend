package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * File Upload Batch View Object
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class FileUploadBatchVO implements Serializable {

    /**
     * Batch ID
     */
    private String batchId;

    /**
     * User name
     */
    private String userName;

    /**
     * User email
     */
    private String userEmail;

    /**
     * Folder type
     */
    private String folderType;

    /**
     * Folder path
     */
    private String folderPath;

    /**
     * Total files
     */
    private Integer totalFiles;

    /**
     * Successful files
     */
    private Integer successfulFiles;

    /**
     * Failed files
     */
    private Integer failedFiles;

    /**
     * Pending files
     */
    private Integer pendingFiles;

    /**
     * Batch status
     */
    private String batchStatus;

    /**
     * Success rate percentage
     */
    private Double successRate;

    /**
     * Create time
     */
    private Date createTime;

    /**
     * Completion time
     */
    private Date completionTime;

    /**
     * Total duration in seconds
     */
    private Long totalDurationSeconds;

    /**
     * File records (optional, included in detail view)
     */
    private List<FileUploadRecordVO> fileRecords;

    /**
     * Concatenated file names for this batch (for history list display),
     * e.g. "a.png, b.jpg, ..."
     * Max length: 40 characters (truncate and append "..." when exceeded)
     */
    private String fileNamesSummary;

    private static final long serialVersionUID = 1L;
}

