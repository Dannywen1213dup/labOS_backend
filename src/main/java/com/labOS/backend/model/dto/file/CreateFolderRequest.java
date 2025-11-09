package com.labOS.backend.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建文件夹请求
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class CreateFolderRequest implements Serializable {

    /**
     * 用户 UUID
     */
    private String uuid;

    private static final long serialVersionUID = 1L;
}

