package com.labOS.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.labOS.backend.model.entity.FileUploadRecord;

import java.util.List;

/**
 * File Upload Record Service
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
public interface FileUploadRecordService extends IService<FileUploadRecord> {

    /**
     * Get file records by batch ID
     *
     * @param batchId Batch ID
     * @return List of file records
     */
    List<FileUploadRecord> getRecordsByBatchId(String batchId);

    /**
     * Update file upload status by batch ID and sanitized file name
     *
     * @param batchId            Batch ID
     * @param sanitizedFileName  Sanitized file name
     * @param status             Upload status
     * @param fileSize           File size (optional)
     * @param errorMessage       Error message (optional)
     * @param errorCode          Error code (optional)
     * @param httpStatusCode     HTTP status code (optional)
     * @return Success or not
     */
    boolean updateUploadStatus(String batchId, String sanitizedFileName, String status,
                              Long fileSize, String errorMessage, String errorCode,
                              Integer httpStatusCode);
}

