package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Upload progress view object
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class UploadProgressVO implements Serializable {

    /**
     * Folder path
     */
    private String folderPath;

    /**
     * Uploaded file count
     */
    private Integer fileCount;

    /**
     * File list
     */
    private java.util.List<String> files;

    private static final long serialVersionUID = 1L;
}

