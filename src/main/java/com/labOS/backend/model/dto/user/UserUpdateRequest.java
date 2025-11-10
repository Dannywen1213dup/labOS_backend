package com.labOS.backend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * User update request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class UserUpdateRequest implements Serializable {
    /**
     * Id
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
     * Profile
     */
    private String userProfile;

    /**
     * User role: user/admin/ban
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}