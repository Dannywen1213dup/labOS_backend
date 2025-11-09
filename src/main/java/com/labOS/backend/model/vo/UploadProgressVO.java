package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传进度视图
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class UploadProgressVO implements Serializable {

    /**
     * 文件夹路径
     */
    private String folderPath;

    /**
     * 已上传的文件数量
     */
    private Integer fileCount;

    /**
     * 文件列表
     */
    private java.util.List<String> files;

    private static final long serialVersionUID = 1L;
}

