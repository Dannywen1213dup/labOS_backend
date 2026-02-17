package com.labOS.backend.model.vo;

import cn.hutool.json.JSONUtil;
import com.labOS.backend.model.entity.Post;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * Post view object
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class PostVO implements Serializable {

    /**
     * Id
     */
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
    private Date createTime;

    /**
     * Update time
     */
    private Date updateTime;

    /**
     * Tag list
     */
    private List<String> tagList;

    /**
     * Creator user information
     */
    private UserVO user;

    /**
     * Has thumbed
     */
    private Boolean hasThumb;

    /**
     * Has favoured
     */
    private Boolean hasFavour;

    /**
     * Convert VO to entity
     *
     * @param postVO
     * @return
     */
    public static Post voToObj(PostVO postVO) {
        if (postVO == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postVO, post);
        List<String> tagList = postVO.getTagList();
        post.setTags(JSONUtil.toJsonStr(tagList));
        return post;
    }

    /**
     * Convert entity to VO
     *
     * @param post
     * @return
     */
    public static PostVO objToVo(Post post) {
        if (post == null) {
            return null;
        }
        PostVO postVO = new PostVO();
        BeanUtils.copyProperties(post, postVO);
        postVO.setTagList(JSONUtil.toList(post.getTags(), String.class));
        return postVO;
    }
}
