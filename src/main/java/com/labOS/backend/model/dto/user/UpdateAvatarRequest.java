package com.labOS.backend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * Request to update user avatar after presigned upload completes
 */
@Data
public class UpdateAvatarRequest implements Serializable {

    /**
     * S3 object key for the avatar
     */
    private String avatarKey;

    private static final long serialVersionUID = 1L;
}
