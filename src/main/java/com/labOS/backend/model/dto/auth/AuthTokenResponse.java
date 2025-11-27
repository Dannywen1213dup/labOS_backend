package com.labOS.backend.model.dto.auth;

import com.labOS.backend.model.vo.LoginUserVO;
import java.io.Serializable;
import lombok.Data;

/**
 * Auth token response
 * Returned after successful login or registration verification
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class AuthTokenResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Token name (e.g., "satoken")
     */
    private String tokenName;

    /**
     * Token value (the actual token string)
     */
    private String tokenValue;

    /**
     * Whether user is logged in
     */
    private Boolean isLogin;

    /**
     * User ID
     */
    private String loginId;

    /**
     * Token timeout in seconds
     */
    private Long tokenTimeout;

    /**
     * User profile information
     */
    private LoginUserVO userProfile;
}

