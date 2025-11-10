package com.labOS.backend.model.dto.file;

import java.io.Serializable;
import lombok.Data;

/**
 * Upload file request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class UploadFileRequest implements Serializable {

    /**
     * Business type
     */
    private String biz;

    private static final long serialVersionUID = 1L;
}