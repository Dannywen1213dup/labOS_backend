package com.labOS.backend.model.dto.post;

import com.labOS.backend.common.PageRequest;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Query post request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PostQueryRequest extends PageRequest implements Serializable {

    /**
     * Id
     */
    private Long id;

    /**
     * Not id
     */
    private Long notId;

    /**
     * Search text
     */
    private String searchText;

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

    /**
     * At least one tag
     */
    private List<String> orTags;

    /**
     * Creator user id
     */
    private Long userId;

    /**
     * Favour user id
     */
    private Long favourUserId;

    private static final long serialVersionUID = 1L;
}