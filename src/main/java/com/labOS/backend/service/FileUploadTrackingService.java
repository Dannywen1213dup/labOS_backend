package com.labOS.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.labOS.backend.model.dto.file.UploadHistoryQueryRequest;
import com.labOS.backend.model.entity.FileUploadBatch;
import com.labOS.backend.model.entity.FileUploadRecord;
import com.labOS.backend.model.vo.FileUploadBatchVO;
import com.labOS.backend.model.vo.UploadStatisticsVO;

import java.util.List;

/**
 * File Upload Tracking Service
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
public interface FileUploadTrackingService extends IService<FileUploadBatch> {

    /**
     * Create a new upload batch with file records
     *
     * @param batch        Batch entity
     * @param fileRecords  List of file records
     * @return Batch ID
     */
    String createUploadBatch(FileUploadBatch batch, List<FileUploadRecord> fileRecords);

    /**
     * Update file upload status
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
    boolean updateFileUploadStatus(String batchId, String sanitizedFileName, String status, 
                                   Long fileSize, String errorMessage, String errorCode, 
                                   Integer httpStatusCode);

    /**
     * Get upload history by user ID with pagination
     *
     * @param userId       User ID
     * @param queryRequest Query parameters
     * @return Page of batch VOs
     */
    Page<FileUploadBatchVO> getUploadHistory(Long userId, UploadHistoryQueryRequest queryRequest);

    /**
     * Get batch detail with file records
     *
     * @param batchId  Batch ID
     * @param userId   User ID (for permission check)
     * @return Batch VO with file records
     */
    FileUploadBatchVO getBatchDetail(String batchId, Long userId);

    /**
     * Get upload statistics for user
     *
     * @param userId User ID
     * @return Statistics VO
     */
    UploadStatisticsVO getUploadStatistics(Long userId);

    /**
     * Get query wrapper for upload history
     *
     * @param queryRequest Query request
     * @return Query wrapper
     */
    QueryWrapper<FileUploadBatch> getQueryWrapper(UploadHistoryQueryRequest queryRequest);
}

