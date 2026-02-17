package com.labOS.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.labOS.backend.common.ErrorCode;
import com.labOS.backend.exception.BusinessException;
import com.labOS.backend.mapper.FileUploadBatchMapper;
import com.labOS.backend.model.dto.file.UploadHistoryQueryRequest;
import com.labOS.backend.model.entity.FileUploadBatch;
import com.labOS.backend.model.entity.FileUploadRecord;
import com.labOS.backend.model.vo.FileUploadBatchVO;
import com.labOS.backend.model.vo.FileUploadRecordVO;
import com.labOS.backend.model.vo.UploadStatisticsVO;
import com.labOS.backend.service.FileUploadRecordService;
import com.labOS.backend.service.FileUploadTrackingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * File Upload Tracking Service Implementation
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Service
@Slf4j
public class FileUploadTrackingServiceImpl extends ServiceImpl<FileUploadBatchMapper, FileUploadBatch>
        implements FileUploadTrackingService {

    private static final int FILE_NAMES_SUMMARY_MAX_LEN = 40;

    @Resource
    private FileUploadRecordService fileUploadRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createUploadBatch(FileUploadBatch batch, List<FileUploadRecord> fileRecords) {
        // Save batch
        boolean batchSaved = this.save(batch);
        if (!batchSaved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to create upload batch");
        }

        // Save file records
        if (fileRecords != null && !fileRecords.isEmpty()) {
            boolean recordsSaved = fileUploadRecordService.saveBatch(fileRecords);
            if (!recordsSaved) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to create file records");
            }
        }

        log.info("Created upload batch: {} with {} files", batch.getBatchId(), fileRecords.size());
        return batch.getBatchId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFileUploadStatus(String batchId, String sanitizedFileName, String status,
                                         Long fileSize, String errorMessage, String errorCode,
                                         Integer httpStatusCode) {
        // Update file record status
        boolean fileUpdated = fileUploadRecordService.updateUploadStatus(
                batchId, sanitizedFileName, status, fileSize, errorMessage, errorCode, httpStatusCode
        );

        if (!fileUpdated) {
            log.warn("Failed to update file status for batch: {}, file: {}", batchId, sanitizedFileName);
            return false;
        }

        // Recalculate batch statistics
        List<FileUploadRecord> records = fileUploadRecordService.getRecordsByBatchId(batchId);
        
        int totalFiles = records.size();
        int successfulFiles = 0;
        int failedFiles = 0;
        int pendingFiles = 0;

        for (FileUploadRecord record : records) {
            String recordStatus = record.getUploadStatus();
            if ("SUCCESS".equals(recordStatus)) {
                successfulFiles++;
            } else if ("FAILED".equals(recordStatus) || "EXPIRED".equals(recordStatus)) {
                failedFiles++;
            } else if ("PENDING".equals(recordStatus)) {
                pendingFiles++;
            }
        }

        // Determine batch status
        String batchStatus;
        if (pendingFiles == 0) {
            if (failedFiles == 0) {
                batchStatus = "COMPLETED";
            } else if (successfulFiles == 0) {
                batchStatus = "FAILED";
            } else {
                batchStatus = "PARTIAL_SUCCESS";
            }
        } else {
            batchStatus = "IN_PROGRESS";
        }

        // Update batch
        UpdateWrapper<FileUploadBatch> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("batchId", batchId);
        updateWrapper.set("successfulFiles", successfulFiles);
        updateWrapper.set("failedFiles", failedFiles);
        updateWrapper.set("pendingFiles", pendingFiles);
        updateWrapper.set("batchStatus", batchStatus);
        
        if (pendingFiles == 0) {
            updateWrapper.set("completionTime", new Date());
        }

        boolean batchUpdated = this.update(updateWrapper);
        
        log.info("Updated batch status: {} - Total: {}, Success: {}, Failed: {}, Pending: {}, Status: {}",
                batchId, totalFiles, successfulFiles, failedFiles, pendingFiles, batchStatus);

        return batchUpdated;
    }

    @Override
    public Page<FileUploadBatchVO> getUploadHistory(Long userId, UploadHistoryQueryRequest queryRequest) {
        // Build query wrapper
        QueryWrapper<FileUploadBatch> queryWrapper = this.getQueryWrapper(queryRequest);
        queryWrapper.eq("userId", userId);
        queryWrapper.orderByDesc("createTime");

        // Query with pagination
        long current = queryRequest.getCurrent();
        long pageSize = queryRequest.getPageSize();
        Page<FileUploadBatch> batchPage = this.page(new Page<>(current, pageSize), queryWrapper);

        // Batch collect filenames for all batchIds in this page (avoid N+1)
        List<String> batchIds = batchPage.getRecords().stream()
                .map(FileUploadBatch::getBatchId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        Map<String, List<String>> batchIdToFileNames = Collections.emptyMap();
        if (!batchIds.isEmpty()) {
            List<FileUploadRecord> records = fileUploadRecordService.lambdaQuery()
                    .select(FileUploadRecord::getBatchId, FileUploadRecord::getOriginalFileName)
                    .in(FileUploadRecord::getBatchId, batchIds)
                    .list();
            batchIdToFileNames = new HashMap<>();
            for (FileUploadRecord r : records) {
                if (r == null || StringUtils.isBlank(r.getBatchId()) || StringUtils.isBlank(r.getOriginalFileName())) {
                    continue;
                }
                batchIdToFileNames.computeIfAbsent(r.getBatchId(), k -> new ArrayList<>()).add(r.getOriginalFileName());
            }
        }

        // Convert to VO
        Page<FileUploadBatchVO> voPage = new Page<>(current, pageSize, batchPage.getTotal());
        Map<String, List<String>> finalBatchIdToFileNames = batchIdToFileNames;
        List<FileUploadBatchVO> voList = batchPage.getRecords().stream()
                .map(batch -> {
                    FileUploadBatchVO vo = batchToVO(batch);
                    List<String> names = finalBatchIdToFileNames.getOrDefault(batch.getBatchId(), Collections.emptyList());
                    vo.setFileNamesSummary(buildFileNamesSummary(names));
                    return vo;
                })
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public FileUploadBatchVO getBatchDetail(String batchId, Long userId) {
        // Query batch
        QueryWrapper<FileUploadBatch> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("batchId", batchId);
        queryWrapper.eq("userId", userId);
        FileUploadBatch batch = this.getOne(queryWrapper);

        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Batch not found");
        }

        // Convert to VO
        FileUploadBatchVO vo = batchToVO(batch);

        // Get file records
        List<FileUploadRecord> records = fileUploadRecordService.getRecordsByBatchId(batchId);
        List<FileUploadRecordVO> recordVOs = records.stream()
                .map(this::recordToVO)
                .collect(Collectors.toList());
        vo.setFileRecords(recordVOs);
        // Also set summary for convenience
        List<String> names = records.stream()
                .map(FileUploadRecord::getOriginalFileName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        vo.setFileNamesSummary(buildFileNamesSummary(names));

        return vo;
    }

    @Override
    public UploadStatisticsVO getUploadStatistics(Long userId) {
        UploadStatisticsVO statistics = new UploadStatisticsVO();

        // Query all batches for user
        QueryWrapper<FileUploadBatch> batchQuery = new QueryWrapper<>();
        batchQuery.eq("userId", userId);
        List<FileUploadBatch> batches = this.list(batchQuery);

        // Calculate batch statistics
        statistics.setTotalBatches((long) batches.size());

        long totalFiles = 0;
        long successfulUploads = 0;
        long failedUploads = 0;
        long pendingUploads = 0;
        long datasetFilesCount = 0;
        long benchmarkEvalFilesCount = 0;

        for (FileUploadBatch batch : batches) {
            totalFiles += batch.getTotalFiles();
            successfulUploads += batch.getSuccessfulFiles();
            failedUploads += batch.getFailedFiles();
            pendingUploads += batch.getPendingFiles();

            if ("dataset".equalsIgnoreCase(batch.getFolderType())) {
                datasetFilesCount += batch.getTotalFiles();
            } else if ("benchmark-eval".equalsIgnoreCase(batch.getFolderType())) {
                benchmarkEvalFilesCount += batch.getTotalFiles();
            }
        }

        statistics.setTotalFiles(totalFiles);
        statistics.setSuccessfulUploads(successfulUploads);
        statistics.setFailedUploads(failedUploads);
        statistics.setPendingUploads(pendingUploads);
        statistics.setDatasetFilesCount(datasetFilesCount);
        statistics.setBenchmarkEvalFilesCount(benchmarkEvalFilesCount);

        // Calculate success rate
        if (totalFiles > 0) {
            double successRate = (double) successfulUploads / totalFiles * 100;
            statistics.setOverallSuccessRate(Math.round(successRate * 100.0) / 100.0);
        } else {
            statistics.setOverallSuccessRate(0.0);
        }

        // Calculate total storage
        QueryWrapper<FileUploadRecord> recordQuery = new QueryWrapper<>();
        recordQuery.eq("userId", userId);
        recordQuery.eq("uploadStatus", "SUCCESS");
        List<FileUploadRecord> successRecords = fileUploadRecordService.list(recordQuery);

        long totalBytes = successRecords.stream()
                .filter(r -> r.getFileSize() != null)
                .mapToLong(FileUploadRecord::getFileSize)
                .sum();

        statistics.setTotalStorageBytes(totalBytes);
        statistics.setTotalStorageFormatted(formatFileSize(totalBytes));

        return statistics;
    }

    @Override
    public QueryWrapper<FileUploadBatch> getQueryWrapper(UploadHistoryQueryRequest queryRequest) {
        QueryWrapper<FileUploadBatch> queryWrapper = new QueryWrapper<>();

        if (queryRequest == null) {
            return queryWrapper;
        }

        String batchStatus = queryRequest.getBatchStatus();
        String folderType = queryRequest.getFolderType();
        Date startDate = queryRequest.getStartDate();
        Date endDate = queryRequest.getEndDate();

        // Filter by batch status
        if (StringUtils.isNotBlank(batchStatus)) {
            queryWrapper.eq("batchStatus", batchStatus);
        }

        // Filter by folder type
        if (StringUtils.isNotBlank(folderType)) {
            queryWrapper.eq("folderType", folderType);
        }

        // Filter by date range
        if (startDate != null) {
            queryWrapper.ge("createTime", startDate);
        }
        if (endDate != null) {
            queryWrapper.le("createTime", endDate);
        }

        return queryWrapper;
    }

    /**
     * Convert FileUploadBatch entity to VO
     */
    private FileUploadBatchVO batchToVO(FileUploadBatch batch) {
        FileUploadBatchVO vo = new FileUploadBatchVO();
        BeanUtils.copyProperties(batch, vo);

        // Calculate success rate
        if (batch.getTotalFiles() != null && batch.getTotalFiles() > 0) {
            double rate = (double) batch.getSuccessfulFiles() / batch.getTotalFiles() * 100;
            vo.setSuccessRate(Math.round(rate * 100.0) / 100.0);
        } else {
            vo.setSuccessRate(0.0);
        }

        // Calculate duration
        if (batch.getCreateTime() != null && batch.getCompletionTime() != null) {
            long durationMs = batch.getCompletionTime().getTime() - batch.getCreateTime().getTime();
            vo.setTotalDurationSeconds(durationMs / 1000);
        }

        return vo;
    }

    /**
     * Convert FileUploadRecord entity to VO
     */
    private FileUploadRecordVO recordToVO(FileUploadRecord record) {
        FileUploadRecordVO vo = new FileUploadRecordVO();
        BeanUtils.copyProperties(record, vo);

        // Format file size
        if (record.getFileSize() != null) {
            vo.setFileSizeFormatted(formatFileSize(record.getFileSize()));
        }

        // Calculate duration
        if (record.getRequestTime() != null && record.getUploadCompletionTime() != null) {
            long durationMs = record.getUploadCompletionTime().getTime() - record.getRequestTime().getTime();
            vo.setDurationSeconds(durationMs / 1000);
        }

        return vo;
    }

    private String buildFileNamesSummary(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return "";
        }
        String joined = String.join(", ", fileNames);
        if (joined.length() <= FILE_NAMES_SUMMARY_MAX_LEN) {
            return joined;
        }
        int keep = Math.max(0, FILE_NAMES_SUMMARY_MAX_LEN - 3);
        return joined.substring(0, keep) + "...";
    }

    /**
     * Format file size to human-readable string
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}

