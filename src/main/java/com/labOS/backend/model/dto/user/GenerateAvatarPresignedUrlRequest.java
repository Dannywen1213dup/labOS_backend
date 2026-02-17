package com.labOS.backend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * Request for generating avatar presigned upload URL
 */
@Data
public class GenerateAvatarPresignedUrlRequest implements Serializable {

    /**
     * Version for avatar file name (rule: {version}.{ext})
     */
    private String version;

    /**
     * File extension (e.g., png, jpg, webp)
     */
    private String ext;

    /**
     * Optional URL expiration time in milliseconds
     */
    private Long expirationTime;

    /**
     * Content type of the upload (e.g., image/png)
     */
    private String contentType;

    /**
     * File size in bytes
     */
    private Long sizeBytes;

    private static final long serialVersionUID = 1L;
}
