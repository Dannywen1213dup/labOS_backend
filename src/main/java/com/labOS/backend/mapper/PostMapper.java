package com.labOS.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labOS.backend.model.entity.Post;
import java.util.Date;
import java.util.List;

/**
 * Post database operations
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
public interface PostMapper extends BaseMapper<Post> {

    /**
     * Query the list of posts (including deleted posts).
     */
    List<Post> listPostWithDelete(Date minUpdateTime);

}




