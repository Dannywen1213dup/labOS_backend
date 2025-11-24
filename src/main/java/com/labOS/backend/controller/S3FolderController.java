package com.labOS.backend.controller;

import com.labOS.backend.common.BaseResponse;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.common.ResultUtils;
import com.labOS.backend.constant.FileConstant;
import com.labOS.backend.exception.BusinessException;
import com.labOS.backend.manager.S3Manager;
import com.labOS.backend.model.dto.file.BatchPresignedUrlRequest;
import com.labOS.backend.model.dto.file.BatchUploadRequest;
import com.labOS.backend.model.dto.file.CreateFolderRequest;
import com.labOS.backend.model.dto.file.DeleteFolderRequest;
import com.labOS.backend.model.dto.file.DownloadFolderRequest;
import com.labOS.backend.model.dto.file.GeneratePresignedUrlRequest;
import com.labOS.backend.model.dto.file.UploadProgressRequest;
import com.labOS.backend.model.entity.User;
import com.labOS.backend.model.vo.BatchPresignedUrlVO;
import com.labOS.backend.model.vo.BatchUploadResultVO;
import com.labOS.backend.model.vo.FolderInfoVO;
import com.labOS.backend.model.vo.UploadProgressVO;
import com.labOS.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    // KEEP FOR NOW/REFERENCE
    /**
     * Create or retrieve the upload folder
     * Folder structure: labOS/{uuid}/{MMDDYYYY}/{count}/
     * If the uuid folder does not exist, it will be created automatically.
     * If today's date folder does not exist, it will be created automatically.
     * Automatically create a new count folder.
     *
     * @param createFolderRequest include uuid
     * @return Folder information
     */
    /*
    @PostMapping("/create")
    public BaseResponse<FolderInfoVO> createFolder(@RequestBody CreateFolderRequest createFolderRequest) {
        if (createFolderRequest == null || StringUtils.isBlank(createFolderRequest.getUuid())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "UUID cannot be empty");
        }

        String uuid = createFolderRequest.getUuid();
        
        try {
            // Create folder and get path
            String folderPath = s3Manager.getOrCreateUploadFolder(uuid);
            
            // Parse path information
            FolderInfoVO folderInfoVO = new FolderInfoVO();
            folderInfoVO.setFolderPath(folderPath);
            
            // Parse path: labOS/{uuid}/{MMDDYYYY}/{count}/
            String[] parts = folderPath.split("/");
            if (parts.length >= 4) {
                folderInfoVO.setUuid(parts[1]);
                folderInfoVO.setDate(parts[2]);
                folderInfoVO.setCount(Integer.parseInt(parts[3]));
            }
            
            log.info("Created folder successfully: {}", folderPath);
            return ResultUtils.success(folderInfoVO);
        } catch (Exception e) {
            log.error("Failed to create folder for uuid: {}", uuid, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to create folder");
        }
    }

    /**
     * Delete the folder and all its contents.
     *
     * @param deleteFolderRequest Contains folder path
     * @return is successfully deleted
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteFolder(@RequestBody DeleteFolderRequest deleteFolderRequest) {
        if (deleteFolderRequest == null || StringUtils.isBlank(deleteFolderRequest.getFolderPath())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The folder path cannot be empty");
        }

        String folderPath = deleteFolderRequest.getFolderPath();
        
        // Verify path format
        if (!isValidFolderPath(folderPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The folder path format is incorrect. The correct format is:: labOS/{uuid}/{MMDDYYYY}/{count}");
        }

        try {
            // Check if the folder exists
            if (!s3Manager.doesFolderExist(folderPath)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Folder does not exist");
            }
            
            s3Manager.deleteFolder(folderPath);
            log.info("Deleted folder successfully: {}", folderPath);
            return ResultUtils.success(true);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete folder: {}", folderPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to delete folder");
        }
    }

    /**
     *  Get the folder download link (ZIP format）
     *  Returns a temporary URL that your browser can directly access to download.
     *
     * @param downloadFolderRequest Contains folder path
     * @return downloads URL
     */
    @PostMapping("/download")
    public BaseResponse<String> downloadFolder(@RequestBody DownloadFolderRequest downloadFolderRequest) {
        if (downloadFolderRequest == null || StringUtils.isBlank(downloadFolderRequest.getFolderPath())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The folder path cannot be empty.");
        }

        String folderPath = downloadFolderRequest.getFolderPath();
        
        // Verify path format
        if (!isValidFolderPath(folderPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The folder path format is incorrect. The correct format is: labOS/{uuid}/{MMDDYYYY}/{count}");
        }

        File zipFile = null;
        try {
            // Check if folder exists
            if (!s3Manager.doesFolderExist(folderPath)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Folder does not exist");
            }

            // Download folder and package as ZIP
            zipFile = s3Manager.downloadFolderAsZip(folderPath);
            
            // Generate ZIP file name
            String zipFileName = folderPath.replace("/", "_") + ".zip";
            
            // Upload ZIP file to S3
            s3Manager.putObject("downloads/" + zipFileName, zipFile);
            
            // Generate presigned URL (1 hour expiration)
            String presignedUrl = s3Manager.generatePresignedUrl("downloads/" + zipFileName, FileConstant.PRESIGNED_URL_EXPIRATION);
            
            log.info("Generated download URL for folder: {}", folderPath);
            return ResultUtils.success(presignedUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate download URL for folder: {}", folderPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to generate download URL");
        } finally {
            // Clean up temporary files
            if (zipFile != null && zipFile.exists()) {
                boolean deleted = zipFile.delete();
                if (!deleted) {
                    log.warn("Failed to delete temporary ZIP file: {}", zipFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Query upload progress
     * Returns the number of uploaded files and file list in the folder
     *
     * @param uploadProgressRequest Contains folder path
     * @return Upload progress information
     */
    @PostMapping("/progress")
    public BaseResponse<UploadProgressVO> getUploadProgress(@RequestBody UploadProgressRequest uploadProgressRequest) {
        if (uploadProgressRequest == null || StringUtils.isBlank(uploadProgressRequest.getFolderPath())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Folder path cannot be empty");
        }

        String folderPath = uploadProgressRequest.getFolderPath();
        
        // Verify path format
        if (!isValidFolderPath(folderPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Folder path format is incorrect. Correct format: labOS/{uuid}/{MMDDYYYY}/{count}");
        }

        try {
            // Check if folder exists
            if (!s3Manager.doesFolderExist(folderPath)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Folder does not exist");
            }

            // Get file count
            int fileCount = s3Manager.getUploadProgress(folderPath);
            
            // Get file list
            List<String> files = s3Manager.listFiles(folderPath);
            
            // Build return object
            UploadProgressVO progressVO = new UploadProgressVO();
            progressVO.setFolderPath(folderPath);
            progressVO.setFileCount(fileCount);
            progressVO.setFiles(files);
            
            log.info("Retrieved upload progress for folder: {}, file count: {}", folderPath, fileCount);
            return ResultUtils.success(progressVO);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get upload progress for folder: {}", folderPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to get upload progress");
        }
    }

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

        // Get logged-in user's ID
        User loginUser = userService.getLoginUser(request);
        String userId = String.valueOf(loginUser.getId());

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

        // Get logged-in user's ID
        User loginUser = userService.getLoginUser(request);
        String userId = String.valueOf(loginUser.getId());

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

        // Get logged-in user's ID
        User loginUser = userService.getLoginUser(request);
        String userId = String.valueOf(loginUser.getId());

        // Create benchmark-eval folder: labOS/benchmark-eval/{userId}/
        String folderPath = s3Manager.getOrCreateBenchmarkEvalFolder(userId);
        log.info("Using benchmark-eval folder: {}", folderPath);

        // Get expiration time (default: 1 hour)
        long expirationTime = batchPresignedUrlRequest.getExpirationTime() != null
                ? batchPresignedUrlRequest.getExpirationTime()
                : FileConstant.PRESIGNED_URL_EXPIRATION;

        // Generate presigned URLs for each file
        List<BatchPresignedUrlVO.PresignedUrlEntry> entries = new ArrayList<>();
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

            log.info("Generated benchmark-eval presigned upload URL for: {}", s3Key);
        }

        // Build response
        BatchPresignedUrlVO response = new BatchPresignedUrlVO();
        response.setEntries(entries);

        log.info("Generated {} benchmark-eval presigned upload URLs", entries.size());
        return ResultUtils.success(response);
    }

    // KEEP FOR NOW/REFERENCE
    /*
    /**
     * Generate presigned URL for uploading objects to S3
     * 
     * This endpoint generates a presigned URL that allows clients to upload files directly to S3
     * without going through the backend server. The URL is valid for a specified expiration time.
     * 
     * <p>Features:
     * <ul>
     *   <li>Automatically creates folder based on logged-in user's ID</li>
     *   <li>Sanitizes filename to ensure S3 bucket safety (removes special characters)</li>
     *   <li>Supports custom expiration time (default: 1 hour)</li>
     *   <li>Returns presigned URL ready for direct client-side uploads to S3</li>
     * </ul>
     * 
     * <p>Folder structure: labOS/{userId}/{MMDDYYYY}/{count}/{sanitizedFileName}
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
    /*
    @PostMapping("/presigned-upload-url")
    public BaseResponse<String> generatePresignedUploadUrl(
            @RequestBody GeneratePresignedUrlRequest generatePresignedUrlRequest,
            HttpServletRequest request) {
        
        if (generatePresignedUrlRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request cannot be null");
        }

        String fileName = generatePresignedUrlRequest.getFileName();
        if (StringUtils.isBlank(fileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "File name is required");
        }

        // Get logged-in user's ID
        User loginUser = userService.getLoginUser(request);
        String userId = String.valueOf(loginUser.getId());

        // Create folder structure based on user ID: labOS/{userId}/{MMDDYYYY}/{count}/
        String folderPath = s3Manager.getOrCreateUploadFolder(userId);
        log.info("Created folder for presigned URL: {}", folderPath);

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

        log.info("Generated presigned upload URL for: {}", s3Key);
        return ResultUtils.success(presignedUrl);
    }
    */

    // KEEP FOR NOW/REFERENCE
    /**
     * Batch upload files to specified folder
     * If folderPath is not provided, a new folder will be created automatically
     * If folderPath is provided, files will be uploaded to the specified folder
     *
     * @param files File array
     * @param uuid User UUID (required)
     * @param folderPath Folder path (optional)
     * @return Upload result
     */
    /*
    @PostMapping("/batch-upload")
    public BaseResponse<BatchUploadResultVO> batchUpload(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam("uuid") String uuid,
            @RequestParam(value = "folderPath", required = false) String folderPath) {
        
        if (StringUtils.isBlank(uuid)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "UUID cannot be empty");
        }

        if (files == null || files.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Please upload at least one file");
        }

        BatchUploadResultVO result = new BatchUploadResultVO();
        List<String> successFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            // If folderPath is not provided, automatically create a new folder
            String targetFolderPath;
            if (StringUtils.isBlank(folderPath)) {
                targetFolderPath = s3Manager.getOrCreateUploadFolder(uuid);
                log.info("Created new folder for batch upload: {}", targetFolderPath);
            } else {
                // Verify provided folder path format
                if (!isValidFolderPath(folderPath)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "Folder path format is incorrect. Correct format: labOS/{uuid}/{MMDDYYYY}/{count}");
                }
                
                // Ensure folder ends with /
                targetFolderPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";
                
                // Check if folder exists, create if not
                if (!s3Manager.doesFolderExist(targetFolderPath)) {
                    s3Manager.createFolder(targetFolderPath);
                    log.info("Created folder for batch upload: {}", targetFolderPath);
                }
            }

            // Upload each file
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    failedFiles.add(file.getOriginalFilename() + " (File is empty)");
                    failCount++;
                    continue;
                }

                File tempFile = null;
                try {
                    // Build file path
                    String fileName = file.getOriginalFilename();
                    String fileKey = targetFolderPath + fileName;

                    // Create temporary file
                    tempFile = File.createTempFile("upload-", "-" + fileName);
                    file.transferTo(tempFile);

                    // Upload to S3
                    s3Manager.putObject(fileKey, tempFile);
                    
                    successFiles.add(fileKey);
                    successCount++;
                    log.info("Successfully uploaded file: {}", fileKey);
                } catch (Exception e) {
                    failedFiles.add(file.getOriginalFilename() + " (Upload failed: " + e.getMessage() + ")");
                    failCount++;
                    log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                } finally {
                    // Clean up temporary files
                    if (tempFile != null && tempFile.exists()) {
                        boolean deleted = tempFile.delete();
                        if (!deleted) {
                            log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                        }
                    }
                }
            }

            // Build return result
            result.setFolderPath(targetFolderPath);
            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setSuccessFiles(successFiles);
            result.setFailedFiles(failedFiles);

            // Parse folder information
            FolderInfoVO folderInfo = new FolderInfoVO();
            folderInfo.setFolderPath(targetFolderPath);
            String[] parts = targetFolderPath.split("/");
            if (parts.length >= 4) {
                folderInfo.setUuid(parts[1]);
                folderInfo.setDate(parts[2]);
                folderInfo.setCount(Integer.parseInt(parts[3]));
            }
            result.setFolderInfo(folderInfo);

            log.info("Batch upload completed. Success: {}, Failed: {}", successCount, failCount);
            return ResultUtils.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Batch upload failed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Batch upload failed");
        }
    }
    */

    /**
     * Validate folder path format
     * Correct format: labOS/{uuid}/{MMDDYYYY}/{count}
     *
     * @param folderPath Folder path
     * @return Whether it is valid
     */
    private boolean isValidFolderPath(String folderPath) {
        if (StringUtils.isBlank(folderPath)) {
            return false;
        }
        
        // Remove leading and trailing slashes
        String path = folderPath.trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        // Split path
        String[] parts = path.split("/");
        
        // Check if there are 4 parts: labOS, uuid, date, count
        if (parts.length != 4) {
            return false;
        }
        
        // Check if first part is labOS
        if (!"labOS".equals(parts[0])) {
            return false;
        }
        
        // Check date format (MMDDYYYY - 8 digits)
        if (!parts[2].matches("\\d{8}")) {
            return false;
        }
        
        // Check if count is a number
        if (!parts[3].matches("\\d+")) {
            return false;
        }
        
        return true;
    }
}

