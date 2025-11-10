package com.labOS.backend.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * Download folder request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class DownloadFolderRequest implements Serializable {

    /**
     * Folder path
     * Format: labOS/{uuid}/{MMDDYYYY}/{count}
     */
    private String folderPath;

    private static final long serialVersionUID = 1L;
}

