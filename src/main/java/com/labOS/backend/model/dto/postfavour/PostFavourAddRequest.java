package com.labOS.backend.model.dto.postfavour;

import java.io.Serializable;
import lombok.Data;

/**
 * Post favour / unfavour request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class PostFavourAddRequest implements Serializable {

    /**
     * Post id
     */
    private Long postId;

    private static final long serialVersionUID = 1L;
}