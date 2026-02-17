package com.labOS.backend.model.dto.auth;

import java.io.Serializable;
import lombok.Data;

/**
 * Check email response
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class CheckEmailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Whether the email exists in the system
     * true - proceed to login flow
     * false - proceed to registration flow
     */
    private Boolean exists;

    public CheckEmailResponse(Boolean exists) {
        this.exists = exists;
    }
}

