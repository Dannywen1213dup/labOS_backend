package com.labOS.backend.model.vo;

import java.io.Serializable;
import lombok.Data;

/**
 * Avatar presigned URL response
 */
@Data
public class AvatarPresignedUrlVO implements Serializable {

    /**
     * Upload method (POST)
     */
    private String uploadMethod;

    /**
     * Upload URL for POST
     */
    private String uploadUrl;

    /**
     * Presigned form fields for POST upload
     */
    private java.util.Map<String, String> formFields;

    /**
     * Presigned URL for PUT upload (deprecated)
     */
    private String presignedUrl;

    /**
     * S3 object key for the avatar
     */
    private String avatarKey;

    /**
     * CloudFront URL for the avatar
     */
    private String avatarUrl;

    private static final long serialVersionUID = 1L;
}
