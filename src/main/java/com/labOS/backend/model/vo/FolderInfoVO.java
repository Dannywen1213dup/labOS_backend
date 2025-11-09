package com.labOS.backend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件夹信息视图
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class FolderInfoVO implements Serializable {

    /**
     * 文件夹路径
     */
    private String folderPath;

    /**
     * UUID
     */
    private String uuid;

    /**
     * 日期 (MMDDYYYY格式)
     */
    private String date;

    /**
     * 次数
     */
    private Integer count;

    private static final long serialVersionUID = 1L;
}

