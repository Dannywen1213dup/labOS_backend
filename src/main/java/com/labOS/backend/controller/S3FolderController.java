package com.labOS.backend.controller;

import com.labOS.backend.common.BaseResponse;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.common.ResultUtils;
import com.labOS.backend.config.S3ClientConfig;
import com.labOS.backend.constant.FileConstant;
import com.labOS.backend.exception.BusinessException;
import com.labOS.backend.manager.S3Manager;
import com.labOS.backend.model.dto.file.BatchPresignedUrlRequest;
import com.labOS.backend.model.dto.file.GeneratePresignedUrlRequest;
import com.labOS.backend.model.entity.FileUploadBatch;
import com.labOS.backend.model.entity.FileUploadRecord;
import com.labOS.backend.model.entity.User;
import com.labOS.backend.model.vo.BatchPresignedUrlVO;
import com.labOS.backend.service.FileUploadTrackingService;
import com.labOS.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * S3 Folder Management Interface
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@RestController
@RequestMapping("/s3/folder")
@Slf4j
public class S3FolderController {

    @Resource
    private S3Manager s3Manager;

    @Resource
    private UserService userService;

    @Resource
    private FileUploadTrackingService fileUploadTrackingService;

    @Resource
    private S3ClientConfig s3ClientConfig;

    // KEEP FOR NOW/REFERENCE
    /**
     * Generate presigned URL for uploading dataset files to S3
     * 
     * This endpoint generates a presigned URL that allows clients to upload dataset files directly to S3.
     * Files are stored in: bucket/labOS/datasets/{userId}/{sanitizedFileName}
     * 
     * <p>Features:
     * <ul>
     *   <li>Automatically creates dataset folder based on logged-in user's ID</li>
     *   <li>Sanitizes filename to ensure S3 bucket safety (removes special characters and SQL injection patterns)</li>
     *   <li>Supports custom expiration time (default: 1 hour)</li>
     *   <li>Returns presigned URL ready for direct client-side uploads to S3</li>
     * </ul>
     * 
     * <p>Folder structure: labOS/datasets/{userId}/{sanitizedFileName}
     * 
     * <p>Request parameters:
     * <ul>
     *   <li>fileName (required): Name of the file to upload (will be sanitized)</li>
     *   <li>expirationTime (optional): URL expiration in milliseconds (default: 3600000 = 1 hour)</li>
     * </ul>
     * 
     * <p>Response: Presigned URL string that can be used with PUT request to upload the file
     *
     * @param generatePresignedUrlRequest request containing file name and optional expiration time
     * @param request HTTP request (used to get logged-in user ID)
     * @return BaseResponse containing the presigned URL for upload
     */
    @PostMapping("/presigned-upload-url/dataset")
    public BaseResponse<String> generateDatasetPresignedUrl(
            @RequestBody GeneratePresignedUrlRequest generatePresignedUrlRequest,
            HttpServletRequest request) {
        
        if (generatePresignedUrlRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request cannot be null");
        }

        String fileName = generatePresignedUrlRequest.getFileName();
        if (StringUtils.isBlank(fileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "File name is required");
        }

        // Use Sa-Token to verify login and get user information
        com.labOS.backend.satoken.SaTokenUtil.checkLogin();
        com.labOS.backend.model.vo.LoginUserVO loginUser = com.labOS.backend.satoken.SaTokenUtil.getUser();
        
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "User not logged in");
        }
        
        String userId = String.valueOf(loginUser.getId());
        log.info("Dataset upload request - User: {}, UserName: {}, Role: {}", 
                userId, loginUser.getUserName(), loginUser.getUserRole());

        // Create dataset folder: labOS/datasets/{userId}/
        String folderPath = s3Manager.getOrCreateDatasetFolder(userId);
        log.info("Using dataset folder: {}", folderPath);

        // Sanitize filename to ensure S3 bucket safety
        String sanitizedFileName = s3Manager.sanitizeFileName(fileName);
        log.info("Sanitized filename: {} -> {}", fileName, sanitizedFileName);

        // Build full S3 key (folder path + sanitized file name)
        String s3Key = folderPath + sanitizedFileName;

        // Get expiration time (default: 1 hour)
        long expirationTime = generatePresignedUrlRequest.getExpirationTime() != null
                ? generatePresignedUrlRequest.getExpirationTime()
                : FileConstant.PRESIGNED_URL_EXPIRATION;

        // Generate presigned URL for PUT/upload
        String presignedUrl = s3Manager.generatePresignedUploadUrl(s3Key, expirationTime);

        log.info("Generated dataset presigned upload URL for: {}", s3Key);
        return ResultUtils.success(presignedUrl);
    }

    /**
     * Generate presigned URL for uploading benchmark evaluation files to S3
     * 
     * This endpoint generates a presigned URL that allows clients to upload benchmark evaluation files directly to S3.
     * Files are stored in: bucket/labOS/benchmark-eval/{userId}/{sanitizedFileName}
     * 
     * <p>Features:
     * <ul>
     *   <li>Automatically creates benchmark-eval folder based on logged-in user's ID</li>
     *   <li>Sanitizes filename to ensure S3 bucket safety (removes special characters and SQL injection patterns)</li>
     *   <li>Supports custom expiration time (default: 1 hour)</li>
     *   <li>Returns presigned URL ready for direct client-side uploads to S3</li>
     * </ul>
     * 
     * <p>Folder structure: labOS/benchmark-eval/{userId}/{sanitizedFileName}
     * 
     * <p>Request parameters:
     * <ul>
     *   <li>fileName (required): Name of the file to upload (will be sanitized)</li>
     *   <li>expirationTime (optional): URL expiration in milliseconds (default: 3600000 = 1 hour)</li>
     * </ul>
     * 
     * <p>Response: Presigned URL string that can be used with PUT request to upload the file
     *
     * @param generatePresignedUrlRequest request containing file name and optional expiration time
     * @param request HTTP request (used to get logged-in user ID)
     * @return BaseResponse containing the presigned URL for upload
     */
    @PostMapping("/presigned-upload-url/benchmark-eval")
    public BaseResponse<String> generateBenchmarkEvalPresignedUrl(
            @RequestBody GeneratePresignedUrlRequest generatePresignedUrlRequest,
            HttpServletRequest request) {
        
        if (generatePresignedUrlRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request cannot be null");
        }

        String fileName = generatePresignedUrlRequest.getFileName();
        if (StringUtils.isBlank(fileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "File name is required");
        }

        // Use Sa-Token to verify login and get user information
        com.labOS.backend.satoken.SaTokenUtil.checkLogin();
        com.labOS.backend.model.vo.LoginUserVO loginUser = com.labOS.backend.satoken.SaTokenUtil.getUser();
        
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "User not logged in");
        }
        
        String userId = String.valueOf(loginUser.getId());
        log.info("Benchmark-eval upload request - User: {}, UserName: {}, Role: {}", 
                userId, loginUser.getUserName(), loginUser.getUserRole());

        // Create benchmark-eval folder: labOS/benchmark-eval/{userId}/
        String folderPath = s3Manager.getOrCreateBenchmarkEvalFolder(userId);
        log.info("Using benchmark-eval folder: {}", folderPath);

        // Sanitize filename to ensure S3 bucket safety
        String sanitizedFileName = s3Manager.sanitizeFileName(fileName);
        log.info("Sanitized filename: {} -> {}", fileName, sanitizedFileName);

        // Build full S3 key (folder path + sanitized file name)
        String s3Key = folderPath + sanitizedFileName;

        // Get expiration time (default: 1 hour)
        long expirationTime = generatePresignedUrlRequest.getExpirationTime() != null
                ? generatePresignedUrlRequest.getExpirationTime()
                : FileConstant.PRESIGNED_URL_EXPIRATION;

        // Generate presigned URL for PUT/upload
        String presignedUrl = s3Manager.generatePresignedUploadUrl(s3Key, expirationTime);

        log.info("Generated benchmark-eval presigned upload URL for: {}", s3Key);
        return ResultUtils.success(presignedUrl);
    }

    /**
     * Generate batch presigned URLs for uploading benchmark evaluation files to S3
     * 
     * This endpoint generates multiple presigned URLs that allow clients to upload benchmark evaluation files directly to S3.
     * Files are stored in: bucket/labOS/benchmark-eval/{userId}/{sanitizedFileName}
     * 
     * <p>Features:
     * <ul>
     *   <li>Automatically creates benchmark-eval folder based on logged-in user's ID</li>
     *   <li>Sanitizes filenames to ensure S3 bucket safety (removes special characters and SQL injection patterns)</li>
     *   <li>Supports custom expiration time (default: 1 hour)</li>
     *   <li>Returns presigned URLs ready for direct client-side uploads to S3</li>
     * </ul>
     * 
     * <p>Folder structure: labOS/benchmark-eval/{userId}/{sanitizedFileName}
     * 
     * <p>Request parameters:
     * <ul>
     *   <li>fileNames (required): List of file names to upload (will be sanitized)</li>
     *   <li>expirationTime (optional): URL expiration in milliseconds (default: 3600000 = 1 hour)</li>
     * </ul>
     * 
     * <p>Response: List of presigned URL entries, each containing original file name, sanitized file name, and presigned URL
     *
     * @param batchPresignedUrlRequest request containing list of file names and optional expiration time
     * @param request HTTP request (used to get logged-in user ID)
     * @return BaseResponse containing list of presigned URLs for upload
     */
    @PostMapping("/presigned-upload-url/benchmark-eval/batch")
    public BaseResponse<BatchPresignedUrlVO> generateBatchBenchmarkEvalPresignedUrls(
            @RequestBody BatchPresignedUrlRequest batchPresignedUrlRequest,
            HttpServletRequest request) {
        
        if (batchPresignedUrlRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request cannot be null");
        }

        List<String> fileNames = batchPresignedUrlRequest.getFileNames();
        if (fileNames == null || fileNames.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "File names list cannot be empty");
        }

        // Use Sa-Token to verify login and get user information
        com.labOS.backend.satoken.SaTokenUtil.checkLogin();
        com.labOS.backend.model.vo.LoginUserVO loginUser = com.labOS.backend.satoken.SaTokenUtil.getUser();
        
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "User not logged in");
        }
        
        String userId = String.valueOf(loginUser.getId());
        
        // Get user details from database to access email
        User userEntity = userService.getById(loginUser.getId());
        if (userEntity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "User not found");
        }
        
        // Log user information including email
        log.info("=== Batch Benchmark-Eval Upload Request ===");
        log.info("User ID: {}", userId);
        log.info("User Name: {}", loginUser.getUserName());
        log.info("User Email: {}", userEntity.getEmail());
        log.info("User Role: {}", loginUser.getUserRole());
        log.info("File Count: {}", fileNames.size());
        log.info("==========================================");

        // Get expiration time (default: 1 hour)
        long expirationTime = batchPresignedUrlRequest.getExpirationTime() != null
                ? batchPresignedUrlRequest.getExpirationTime()
                : FileConstant.PRESIGNED_URL_EXPIRATION;

        // Generate batch ID
        String batchId = UUID.randomUUID().toString();

        // Create benchmark-eval folder with date and batch id:
        // labOS/benchmark-eval/{userId}/{yyyyMMdd}/{batchId}/
        String baseFolderPath = s3Manager.getOrCreateBenchmarkEvalFolder(userId);
        String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String folderPath = baseFolderPath + dateFolder + "/" + batchId + "/";
        try {
            if (!s3Manager.doesFolderExist(folderPath)) {
                s3Manager.createFolder(folderPath);
            }
        } catch (Exception e) {
            log.warn("Failed to create benchmark-eval batch folder marker: {}", folderPath, e);
        }
        log.info("Using benchmark-eval batch folder: {}", folderPath);

        // Create batch entity
        FileUploadBatch batch = new FileUploadBatch();
        batch.setBatchId(batchId);
        batch.setUserId(loginUser.getId());
        batch.setUserEmail(userEntity.getEmail());
        batch.setUserName(loginUser.getUserName());
        batch.setUserRole(loginUser.getUserRole());
        batch.setFolderType("benchmark-eval");
        batch.setFolderPath(folderPath);
        batch.setTotalFiles(fileNames.size());
        batch.setSuccessfulFiles(0);
        batch.setFailedFiles(0);
        batch.setPendingFiles(fileNames.size());
        batch.setBatchStatus("PENDING");
        batch.setExpirationTime(expirationTime);
        batch.setRequestTimezone(batchPresignedUrlRequest.getClientTimezone() != null 
                ? batchPresignedUrlRequest.getClientTimezone() : "UTC");
        batch.setRequestIp(getClientIp(request));
        batch.setUserAgent(request.getHeader("User-Agent"));

        // Generate presigned URLs for each file and create tracking records
        List<BatchPresignedUrlVO.PresignedUrlEntry> entries = new ArrayList<>();
        List<FileUploadRecord> fileRecords = new ArrayList<>();
        
        for (String fileName : fileNames) {
            if (StringUtils.isBlank(fileName)) {
                log.warn("Skipping blank file name in batch request");
                continue;
            }

            // Sanitize filename to ensure S3 bucket safety
            String sanitizedFileName = s3Manager.sanitizeFileName(fileName);
            log.info("Sanitized filename: {} -> {}", fileName, sanitizedFileName);

            // Build full S3 key (folder path + sanitized file name)
            String s3Key = folderPath + sanitizedFileName;

            // Generate presigned URL for PUT/upload
            String presignedUrl = s3Manager.generatePresignedUploadUrl(s3Key, expirationTime);

            // Create entry
            BatchPresignedUrlVO.PresignedUrlEntry entry = new BatchPresignedUrlVO.PresignedUrlEntry();
            entry.setFileName(fileName);
            entry.setSanitizedFileName(sanitizedFileName);
            entry.setPresignedUrl(presignedUrl);
            entries.add(entry);

            // Create file record for tracking
            FileUploadRecord record = new FileUploadRecord();
            record.setBatchId(batchId);
            record.setUserId(loginUser.getId());
            record.setOriginalFileName(fileName);
            record.setSanitizedFileName(sanitizedFileName);
            record.setS3Key(s3Key);
            record.setS3Bucket(s3ClientConfig.getBucket());
            record.setPresignedUrl(presignedUrl);
            record.setUploadStatus("PENDING");
            record.setUploadMethod("PRESIGNED_URL");
            record.setRetryCount(0);
            record.setUrlExpirationTime(new Date(System.currentTimeMillis() + expirationTime));
            record.setClientTimezone(batchPresignedUrlRequest.getClientTimezone() != null 
                    ? batchPresignedUrlRequest.getClientTimezone() : "UTC");
            fileRecords.add(record);

            log.info("Generated benchmark-eval presigned upload URL for: {}", s3Key);
        }

        // Save batch and file records to database
        try {
            fileUploadTrackingService.createUploadBatch(batch, fileRecords);
            log.info("Saved upload tracking for batch: {} with {} files", batchId, fileRecords.size());
        } catch (Exception e) {
            log.error("Failed to save upload tracking for batch: {}", batchId, e);
            // Don't fail the request if tracking fails
        }

        // Build response
        BatchPresignedUrlVO response = new BatchPresignedUrlVO();
        response.setBatchId(batchId);
        response.setEntries(entries);

        log.info("Generated {} benchmark-eval presigned upload URLs for batch: {}", entries.size(), batchId);
        return ResultUtils.success(response);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

