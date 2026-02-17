package com.labOS.backend.model.dto.postthumb;

import java.io.Serializable;
import lombok.Data;

/**
 * Post thumb request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class PostThumbAddRequest implements Serializable {

    /**
     * Post id
     */
    private Long postId;

    private static final long serialVersionUID = 1L;
}