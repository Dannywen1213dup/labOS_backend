package com.labOS.backend.service;

import com.labOS.backend.model.entity.PostThumb;
import com.baomidou.mybatisplus.extension.service.IService;
import com.labOS.backend.model.entity.User;

/**
 *  thumb up service
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
public interface PostThumbService extends IService<PostThumb> {

    /**
     * thumb up
     *
     * @param postId
     * @param loginUser
     * @return
     */
    int doPostThumb(long postId, User loginUser);

    /**
     * thumb up（internal service）
     *
     * @param userId
     * @param postId
     * @return
     */
    int doPostThumbInner(long userId, long postId);
}
