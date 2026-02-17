package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Folder information view object
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class FolderInfoVO implements Serializable {

    /**
     * Folder path
     */
    private String folderPath;

    /**
     * UUID
     */
    private String uuid;

    /**
     * Date (MMDDYYYY format)
     */
    private String date;

    /**
     * Count
     */
    private Integer count;

    private static final long serialVersionUID = 1L;
}

