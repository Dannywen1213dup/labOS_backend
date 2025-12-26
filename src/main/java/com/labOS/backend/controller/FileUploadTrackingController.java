package com.labOS.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labOS.backend.common.BaseResponse;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.common.ResultUtils;
import com.labOS.backend.exception.BusinessException;
import com.labOS.backend.model.dto.file.FileUploadStatusUpdateRequest;
import com.labOS.backend.model.dto.file.UploadHistoryQueryRequest;
import com.labOS.backend.model.vo.FileUploadBatchVO;
import com.labOS.backend.model.vo.UploadStatisticsVO;
import com.labOS.backend.service.FileUploadTrackingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * File Upload Tracking Controller
 * Provides endpoints for tracking and monitoring file uploads
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@RestController
@RequestMapping("/upload-tracking")
@Slf4j
public class FileUploadTrackingController {

    @Resource
    private FileUploadTrackingService fileUploadTrackingService;

    /**
     * Get current user's upload history with pagination and filtering
     * 
     * This endpoint allows users to view their upload history with various filters:
     * - Filter by batch status (PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIAL_SUCCESS)
     * - Filter by folder type (dataset, benchmark-eval)
     * - Filter by date range
     * - Search by file name keyword
     * - Paginated results (default: page 1, size 10)
     * 
     * <p>Response includes:
     * <ul>
     *   <li>Batch information (batch ID, timestamps, status, file counts)</li>
     *   <li>Success rate for each batch</li>
     *   <li>Duration information</li>
     *   <li>User information (name, email, role)</li>
     * </ul>
     *
     * @param queryRequest Query parameters (status, folder type, date range, pagination)
     * @return Paginated list of upload batches
     */
    @PostMapping("/my-uploads")
    public BaseResponse<Page<FileUploadBatchVO>> getMyUploadHistory(
            @RequestBody(required = false) UploadHistoryQueryRequest queryRequest) {
        
        // Create default request if null
        if (queryRequest == null) {
            queryRequest = new UploadHistoryQueryRequest();
        }

        // Ensure pagination defaults are set
        if (queryRequest.getCurrent() <= 0) {
            queryRequest.setCurrent(1);
        }
        if (queryRequest.getPageSize() <= 0) {
            queryRequest.setPageSize(10);
        }
        // Limit maximum page size to prevent performance issues
        if (queryRequest.getPageSize() > 100) {
            queryRequest.setPageSize(100);
        }

        // Use Sa-Token to verify login and get user information
        com.labOS.backend.satoken.SaTokenUtil.checkLogin();
        com.labOS.backend.model.vo.LoginUserVO loginUser = com.labOS.backend.satoken.SaTokenUtil.getUser();
        
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "User not logged in");
        }

        log.info("Fetching upload history for user: {} - page: {}, size: {}", 
                loginUser.getId(), queryRequest.getCurrent(), queryRequest.getPageSize());

        Page<FileUploadBatchVO> historyPage = fileUploadTrackingService.getUploadHistory(
                loginUser.getId(), queryRequest
        );

        return ResultUtils.success(historyPage);
    }

    /**
     * Get detailed information about a specific upload batch
     * 
     * This endpoint provides comprehensive details about a single upload batch including:
     * <ul>
     *   <li>Batch metadata (ID, timestamps, status, user info)</li>
     *   <li>Complete list of all files in the batch</li>
     *   <li>Individual file statuses (PENDING, SUCCESS, FAILED)</li>
     *   <li>Error messages for failed uploads</li>
     *   <li>File sizes and upload durations</li>
     * </ul>
     * 
     * <p>This is useful for:
     * <ul>
     *   <li>Debugging failed uploads</li>
     *   <li>Monitoring upload progress</li>
     *   <li>Viewing detailed file information</li>
     * </ul>
     *
     * @param batchId Batch ID to query
     * @return Detailed batch information with file records
     */
    @GetMapping("/batch/{batchId}")
    public BaseResponse<FileUploadBatchVO> getBatchDetail(@PathVariable String batchId) {
        
        if (StringUtils.isBlank(batchId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Batch ID is required");
        }

        // Use Sa-Token to verify login and get user information
        com.labOS.backend.satoken.SaTokenUtil.checkLogin();
        com.labOS.backend.model.vo.LoginUserVO loginUser = com.labOS.backend.satoken.SaTokenUtil.getUser();
        
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "User not logged in");
        }

        log.info("Fetching batch detail for batch: {} by user: {}", batchId, loginUser.getId());

        FileUploadBatchVO batchDetail = fileUploadTrackingService.getBatchDetail(batchId, loginUser.getId());

        return ResultUtils.success(batchDetail);
    }

    /**
     * Get upload statistics dashboard for current user
     * 
     * This endpoint provides comprehensive statistics about the user's uploads:
     * <ul>
     *   <li>Total number of batches</li>
     *   <li>Total files uploaded (successful, failed, pending)</li>
     *   <li>Overall success rate percentage</li>
     *   <li>Total storage used (in bytes and formatted)</li>
     *   <li>Breakdown by folder type (dataset vs benchmark-eval)</li>
     * </ul>
     * 
     * <p>This is perfect for dashboard displays and monitoring overall upload performance.
     *
     * @return Upload statistics summary
     */
    @GetMapping("/statistics")
    public BaseResponse<UploadStatisticsVO> getUploadStatistics() {
        
        // Use Sa-Token to verify login and get user information
        com.labOS.backend.satoken.SaTokenUtil.checkLogin();
        com.labOS.backend.model.vo.LoginUserVO loginUser = com.labOS.backend.satoken.SaTokenUtil.getUser();
        
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "User not logged in");
        }

        log.info("Fetching upload statistics for user: {}", loginUser.getId());

        UploadStatisticsVO statistics = fileUploadTrackingService.getUploadStatistics(loginUser.getId());

        return ResultUtils.success(statistics);
    }

    /**
     * Update file upload status (callback from frontend after upload completes)
     * 
     * This endpoint is called by the frontend after it completes (or fails) an upload to S3.
     * It updates the tracking record with:
     * <ul>
     *   <li>Upload status (SUCCESS or FAILED)</li>
     *   <li>File size (for successful uploads)</li>
     *   <li>Error information (for failed uploads)</li>
     *   <li>HTTP status code from S3</li>
     * </ul>
     * 
     * <p>The system automatically:
     * <ul>
     *   <li>Updates the individual file record</li>
     *   <li>Recalculates batch statistics (success/fail counts)</li>
     *   <li>Updates batch status (COMPLETED, PARTIAL_SUCCESS, etc.)</li>
     *   <li>Records completion timestamp</li>
     * </ul>
     * 
     * <p>Usage flow:
     * <ol>
     *   <li>Frontend gets presigned URLs from /presigned-upload-url/benchmark-eval/batch</li>
     *   <li>Frontend uploads files to S3 using presigned URLs</li>
     *   <li>Frontend calls this endpoint to report success/failure for each file</li>
     * </ol>
     *
     * @param updateRequest Update request (batch ID, file name, status, error info)
     * @return Success or failure response
     */
    @PostMapping("/update-status")
    public BaseResponse<Boolean> updateUploadStatus(
            @RequestBody FileUploadStatusUpdateRequest updateRequest) {
        
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request cannot be null");
        }

        String batchId = updateRequest.getBatchId();
        String sanitizedFileName = updateRequest.getSanitizedFileName();
        String uploadStatus = updateRequest.getUploadStatus();

        if (StringUtils.isBlank(batchId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Batch ID is required");
        }
        if (StringUtils.isBlank(sanitizedFileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "File name is required");
        }
        if (StringUtils.isBlank(uploadStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Upload status is required");
        }

        // Validate status
        if (!"SUCCESS".equals(uploadStatus) && !"FAILED".equals(uploadStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "Upload status must be SUCCESS or FAILED");
        }

        // Use Sa-Token to verify login
        com.labOS.backend.satoken.SaTokenUtil.checkLogin();
        com.labOS.backend.model.vo.LoginUserVO loginUser = com.labOS.backend.satoken.SaTokenUtil.getUser();
        
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "User not logged in");
        }

        log.info("Updating upload status - User: {}, Batch: {}, File: {}, Status: {}", 
                loginUser.getId(), batchId, sanitizedFileName, uploadStatus);

        boolean success = fileUploadTrackingService.updateFileUploadStatus(
                batchId, 
                sanitizedFileName, 
                uploadStatus,
                updateRequest.getFileSize(),
                updateRequest.getErrorMessage(),
                updateRequest.getErrorCode(),
                updateRequest.getHttpStatusCode()
        );

        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to update upload status");
        }

        return ResultUtils.success(true);
    }
}

