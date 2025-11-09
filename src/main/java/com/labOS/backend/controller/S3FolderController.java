package com.labOS.backend.controller;

import com.labOS.backend.common.BaseResponse;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.common.ResultUtils;
import com.labOS.backend.constant.FileConstant;
import com.labOS.backend.exception.BusinessException;
import com.labOS.backend.manager.S3Manager;
import com.labOS.backend.model.dto.file.BatchUploadRequest;
import com.labOS.backend.model.dto.file.CreateFolderRequest;
import com.labOS.backend.model.dto.file.DeleteFolderRequest;
import com.labOS.backend.model.dto.file.DownloadFolderRequest;
import com.labOS.backend.model.dto.file.UploadProgressRequest;
import com.labOS.backend.model.vo.BatchUploadResultVO;
import com.labOS.backend.model.vo.FolderInfoVO;
import com.labOS.backend.model.vo.UploadProgressVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * S3 文件夹管理接口
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

    /**
     * 创建或获取上传文件夹
     * 文件夹结构: labOS/{uuid}/{MMDDYYYY}/{count}/
     * 如果不存在 uuid 文件夹，会自动创建
     * 如果不存在今天的日期文件夹，会自动创建
     * 自动创建新的次数文件夹
     *
     * @param createFolderRequest 包含 uuid
     * @return 文件夹信息
     */
    @PostMapping("/create")
    public BaseResponse<FolderInfoVO> createFolder(@RequestBody CreateFolderRequest createFolderRequest) {
        if (createFolderRequest == null || StringUtils.isBlank(createFolderRequest.getUuid())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "UUID 不能为空");
        }

        String uuid = createFolderRequest.getUuid();
        
        try {
            // 创建文件夹并获取路径
            String folderPath = s3Manager.getOrCreateUploadFolder(uuid);
            
            // 解析路径信息
            FolderInfoVO folderInfoVO = new FolderInfoVO();
            folderInfoVO.setFolderPath(folderPath);
            
            // 解析路径: labOS/{uuid}/{MMDDYYYY}/{count}/
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建文件夹失败");
        }
    }

    /**
     * 删除文件夹及其所有内容
     *
     * @param deleteFolderRequest 包含文件夹路径
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteFolder(@RequestBody DeleteFolderRequest deleteFolderRequest) {
        if (deleteFolderRequest == null || StringUtils.isBlank(deleteFolderRequest.getFolderPath())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件夹路径不能为空");
        }

        String folderPath = deleteFolderRequest.getFolderPath();
        
        // 验证路径格式
        if (!isValidFolderPath(folderPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件夹路径格式错误，正确格式: labOS/{uuid}/{MMDDYYYY}/{count}");
        }

        try {
            // 检查文件夹是否存在
            if (!s3Manager.doesFolderExist(folderPath)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件夹不存在");
            }
            
            s3Manager.deleteFolder(folderPath);
            log.info("Deleted folder successfully: {}", folderPath);
            return ResultUtils.success(true);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete folder: {}", folderPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除文件夹失败");
        }
    }

    /**
     * 获取文件夹下载链接（ZIP 格式）
     * 返回一个临时 URL，浏览器可以直接访问下载
     *
     * @param downloadFolderRequest 包含文件夹路径
     * @return 下载 URL
     */
    @PostMapping("/download")
    public BaseResponse<String> downloadFolder(@RequestBody DownloadFolderRequest downloadFolderRequest) {
        if (downloadFolderRequest == null || StringUtils.isBlank(downloadFolderRequest.getFolderPath())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件夹路径不能为空");
        }

        String folderPath = downloadFolderRequest.getFolderPath();
        
        // 验证路径格式
        if (!isValidFolderPath(folderPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件夹路径格式错误，正确格式: labOS/{uuid}/{MMDDYYYY}/{count}");
        }

        File zipFile = null;
        try {
            // 检查文件夹是否存在
            if (!s3Manager.doesFolderExist(folderPath)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件夹不存在");
            }

            // 下载文件夹并打包成 ZIP
            zipFile = s3Manager.downloadFolderAsZip(folderPath);
            
            // 生成 ZIP 文件名
            String zipFileName = folderPath.replace("/", "_") + ".zip";
            
            // 上传 ZIP 文件到 S3
            s3Manager.putObject("downloads/" + zipFileName, zipFile);
            
            // 生成预签名 URL（1小时有效期）
            String presignedUrl = s3Manager.generatePresignedUrl("downloads/" + zipFileName, FileConstant.PRESIGNED_URL_EXPIRATION);
            
            log.info("Generated download URL for folder: {}", folderPath);
            return ResultUtils.success(presignedUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate download URL for folder: {}", folderPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成下载链接失败");
        } finally {
            // 清理临时文件
            if (zipFile != null && zipFile.exists()) {
                boolean deleted = zipFile.delete();
                if (!deleted) {
                    log.warn("Failed to delete temporary ZIP file: {}", zipFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 查询上传进度
     * 返回文件夹中已上传的文件数量和文件列表
     *
     * @param uploadProgressRequest 包含文件夹路径
     * @return 上传进度信息
     */
    @PostMapping("/progress")
    public BaseResponse<UploadProgressVO> getUploadProgress(@RequestBody UploadProgressRequest uploadProgressRequest) {
        if (uploadProgressRequest == null || StringUtils.isBlank(uploadProgressRequest.getFolderPath())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件夹路径不能为空");
        }

        String folderPath = uploadProgressRequest.getFolderPath();
        
        // 验证路径格式
        if (!isValidFolderPath(folderPath)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件夹路径格式错误，正确格式: labOS/{uuid}/{MMDDYYYY}/{count}");
        }

        try {
            // 检查文件夹是否存在
            if (!s3Manager.doesFolderExist(folderPath)) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件夹不存在");
            }

            // 获取文件数量
            int fileCount = s3Manager.getUploadProgress(folderPath);
            
            // 获取文件列表
            List<String> files = s3Manager.listFiles(folderPath);
            
            // 构建返回对象
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取上传进度失败");
        }
    }

    /**
     * 批量上传文件到指定文件夹
     * 如果不提供 folderPath，会自动创建新的文件夹
     * 如果提供 folderPath，会上传到指定文件夹
     *
     * @param files 文件数组
     * @param uuid 用户 UUID（必须）
     * @param folderPath 文件夹路径（可选）
     * @return 上传结果
     */
    @PostMapping("/batch-upload")
    public BaseResponse<BatchUploadResultVO> batchUpload(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam("uuid") String uuid,
            @RequestParam(value = "folderPath", required = false) String folderPath) {
        
        if (StringUtils.isBlank(uuid)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "UUID 不能为空");
        }

        if (files == null || files.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请至少上传一个文件");
        }

        BatchUploadResultVO result = new BatchUploadResultVO();
        List<String> successFiles = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            // 如果没有提供 folderPath，自动创建新的文件夹
            String targetFolderPath;
            if (StringUtils.isBlank(folderPath)) {
                targetFolderPath = s3Manager.getOrCreateUploadFolder(uuid);
                log.info("Created new folder for batch upload: {}", targetFolderPath);
            } else {
                // 验证提供的文件夹路径格式
                if (!isValidFolderPath(folderPath)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件夹路径格式错误，正确格式: labOS/{uuid}/{MMDDYYYY}/{count}");
                }
                
                // 确保文件夹以 / 结尾
                targetFolderPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";
                
                // 检查文件夹是否存在，不存在则创建
                if (!s3Manager.doesFolderExist(targetFolderPath)) {
                    s3Manager.createFolder(targetFolderPath);
                    log.info("Created folder for batch upload: {}", targetFolderPath);
                }
            }

            // 上传每个文件
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    failedFiles.add(file.getOriginalFilename() + " (文件为空)");
                    failCount++;
                    continue;
                }

                File tempFile = null;
                try {
                    // 构建文件路径
                    String fileName = file.getOriginalFilename();
                    String fileKey = targetFolderPath + fileName;

                    // 创建临时文件
                    tempFile = File.createTempFile("upload-", "-" + fileName);
                    file.transferTo(tempFile);

                    // 上传到 S3
                    s3Manager.putObject(fileKey, tempFile);
                    
                    successFiles.add(fileKey);
                    successCount++;
                    log.info("Successfully uploaded file: {}", fileKey);
                } catch (Exception e) {
                    failedFiles.add(file.getOriginalFilename() + " (上传失败: " + e.getMessage() + ")");
                    failCount++;
                    log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                } finally {
                    // 清理临时文件
                    if (tempFile != null && tempFile.exists()) {
                        boolean deleted = tempFile.delete();
                        if (!deleted) {
                            log.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
                        }
                    }
                }
            }

            // 构建返回结果
            result.setFolderPath(targetFolderPath);
            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setSuccessFiles(successFiles);
            result.setFailedFiles(failedFiles);

            // 解析文件夹信息
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
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量上传失败");
        }
    }

    /**
     * 验证文件夹路径格式
     * 正确格式: labOS/{uuid}/{MMDDYYYY}/{count}
     *
     * @param folderPath 文件夹路径
     * @return 是否有效
     */
    private boolean isValidFolderPath(String folderPath) {
        if (StringUtils.isBlank(folderPath)) {
            return false;
        }
        
        // 移除首尾的斜杠
        String path = folderPath.trim();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        // 分割路径
        String[] parts = path.split("/");
        
        // 检查是否有 4 个部分: labOS, uuid, date, count
        if (parts.length != 4) {
            return false;
        }
        
        // 检查第一部分是否为 labOS
        if (!"labOS".equals(parts[0])) {
            return false;
        }
        
        // 检查日期格式 (MMDDYYYY - 8位数字)
        if (!parts[2].matches("\\d{8}")) {
            return false;
        }
        
        // 检查次数是否为数字
        if (!parts[3].matches("\\d+")) {
            return false;
        }
        
        return true;
    }
}

