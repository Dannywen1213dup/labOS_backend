package com.labOS.backend.model.dto.auth;

import java.io.Serializable;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * Auth login request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class AuthLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Email address
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * User password
     */
    @NotBlank(message = "Password cannot be blank")
    private String password;
}

