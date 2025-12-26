package com.labOS.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.labOS.backend.mapper.FileUploadRecordMapper;
import com.labOS.backend.model.entity.FileUploadRecord;
import com.labOS.backend.service.FileUploadRecordService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * File Upload Record Service Implementation
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Service
public class FileUploadRecordServiceImpl extends ServiceImpl<FileUploadRecordMapper, FileUploadRecord>
        implements FileUploadRecordService {

    @Override
    public List<FileUploadRecord> getRecordsByBatchId(String batchId) {
        QueryWrapper<FileUploadRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("batchId", batchId);
        queryWrapper.orderByAsc("requestTime");
        return this.list(queryWrapper);
    }

    @Override
    public boolean updateUploadStatus(String batchId, String sanitizedFileName, String status,
                                      Long fileSize, String errorMessage, String errorCode,
                                      Integer httpStatusCode) {
        UpdateWrapper<FileUploadRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("batchId", batchId);
        updateWrapper.eq("sanitizedFileName", sanitizedFileName);
        
        updateWrapper.set("uploadStatus", status);
        updateWrapper.set("uploadCompletionTime", new Date());
        
        if (fileSize != null) {
            updateWrapper.set("fileSize", fileSize);
        }
        if (errorMessage != null) {
            updateWrapper.set("errorMessage", errorMessage);
        }
        if (errorCode != null) {
            updateWrapper.set("errorCode", errorCode);
        }
        if (httpStatusCode != null) {
            updateWrapper.set("httpStatusCode", httpStatusCode);
        }
        
        return this.update(updateWrapper);
    }
}

