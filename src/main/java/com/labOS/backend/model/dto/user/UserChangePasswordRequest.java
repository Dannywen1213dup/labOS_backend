package com.labOS.backend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * Change password (logged-in user)
 */
@Data
public class UserChangePasswordRequest implements Serializable {

    /**
     * Old password
     */
    private String oldPassword;

    /**
     * New password
     */
    private String newPassword;

    /**
     * Confirm new password
     */
    private String confirmPassword;

    private static final long serialVersionUID = 1L;
}


