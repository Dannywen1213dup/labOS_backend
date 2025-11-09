package com.labOS.backend.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量上传文件请求
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class BatchUploadRequest implements Serializable {

    /**
     * 用户 UUID
     */
    private String uuid;

    /**
     * 文件夹路径（可选，如果不提供则自动创建新的）
     * 格式: labOS/{uuid}/{MMDDYYYY}/{count}
     */
    private String folderPath;

    private static final long serialVersionUID = 1L;
}

