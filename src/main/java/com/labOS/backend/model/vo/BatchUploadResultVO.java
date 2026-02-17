package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Batch upload result view object
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class BatchUploadResultVO implements Serializable {

    /**
     * Folder path
     */
    private String folderPath;

    /**
     * Successfully uploaded file count
     */
    private Integer successCount;

    /**
     * Failed upload file count
     */
    private Integer failCount;

    /**
     * Successfully uploaded file path list
     */
    private List<String> successFiles;

    /**
     * Failed upload file name list
     */
    private List<String> failedFiles;

    /**
     * Folder information
     */
    private FolderInfoVO folderInfo;

    private static final long serialVersionUID = 1L;
}

