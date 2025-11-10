package com.labOS.backend.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * Logged-in user view object (desensitized)
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 **/
@Data
public class LoginUserVO implements Serializable {

    /**
     * User id
     */
    private Long id;

    /**
     * User nickname
     */
    private String userName;

    /**
     * User avatar
     */
    private String userAvatar;

    /**
     * User profile
     */
    private String userProfile;

    /**
     * User role: user/admin/ban
     */
    private String userRole;

    /**
     * Create time
     */
    private Date createTime;

    /**
     * Update time
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}