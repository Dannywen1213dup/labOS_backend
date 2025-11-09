package com.labOS.backend.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询上传进度请求
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class UploadProgressRequest implements Serializable {

    /**
     * 文件夹路径
     * 格式: labOS/{uuid}/{MMDDYYYY}/{count}
     */
    private String folderPath;

    private static final long serialVersionUID = 1L;
}

