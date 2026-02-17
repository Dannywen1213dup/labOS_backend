package com.labOS.backend.model.dto.post;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.labOS.backend.model.entity.Post;
import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Post ES wrapper class
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 **/
// todo Uncomment to enable ES (ES must be configured first)
//@Document(indexName = "post")
@Data
public class PostEsDTO implements Serializable {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * Id
     */
    @Id
    private Long id;

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
     * Thumb count
     */
    private Integer thumbNum;

    /**
     * Favour count
     */
    private Integer favourNum;

    /**
     * Creator user id
     */
    private Long userId;

    /**
     * Create time
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date createTime;

    /**
     * Update time
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date updateTime;

    /**
     * Is deleted
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;

    /**
     * Convert entity to DTO
     *
     * @param post
     * @return
     */
    public static PostEsDTO objToDto(Post post) {
        if (post == null) {
            return null;
        }
        PostEsDTO postEsDTO = new PostEsDTO();
        BeanUtils.copyProperties(post, postEsDTO);
        String tagsStr = post.getTags();
        if (StringUtils.isNotBlank(tagsStr)) {
            postEsDTO.setTags(JSONUtil.toList(tagsStr, String.class));
        }
        return postEsDTO;
    }

    /**
     * Convert DTO to entity
     *
     * @param postEsDTO
     * @return
     */
    public static Post dtoToObj(PostEsDTO postEsDTO) {
        if (postEsDTO == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postEsDTO, post);
        List<String> tagList = postEsDTO.getTags();
        if (CollUtil.isNotEmpty(tagList)) {
            post.setTags(JSONUtil.toJsonStr(tagList));
        }
        return post;
    }
}
