package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量上传结果视图
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class BatchUploadResultVO implements Serializable {

    /**
     * 文件夹路径
     */
    private String folderPath;

    /**
     * 成功上传的文件数量
     */
    private Integer successCount;

    /**
     * 失败上传的文件数量
     */
    private Integer failCount;

    /**
     * 上传成功的文件路径列表
     */
    private List<String> successFiles;

    /**
     * 上传失败的文件名列表
     */
    private List<String> failedFiles;

    /**
     * 文件夹信息
     */
    private FolderInfoVO folderInfo;

    private static final long serialVersionUID = 1L;
}

