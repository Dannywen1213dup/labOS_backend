package com.labOS.backend.model.dto.post;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * Create post request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class PostAddRequest implements Serializable {

    /**
     * Title
     */
    private String title;

    /**
     * Content
     */
    private String content;

    /**
     * Tag list
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}