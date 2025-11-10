package com.labOS.backend.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * Batch upload file request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class BatchUploadRequest implements Serializable {

    /**
     * User UUID
     */
    private String uuid;

    /**
     * Folder path (optional, if not provided, a new one will be created automatically)
     * Format: labOS/{uuid}/{MMDDYYYY}/{count}
     */
    private String folderPath;

    private static final long serialVersionUID = 1L;
}

