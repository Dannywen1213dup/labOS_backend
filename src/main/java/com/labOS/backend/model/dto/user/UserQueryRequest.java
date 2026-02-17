package com.labOS.backend.model.dto.user;

import com.labOS.backend.common.PageRequest;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User query request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    /**
     * Id
     */
    private Long id;

    /**
     * Open platform id
     */
    private String unionId;

    /**
     * Official account openId
     */
    private String mpOpenId;

    /**
     * User nickname
     */
    private String userName;

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