package com.labOS.backend.model.dto.auth;

import java.io.Serializable;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Register verification request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class RegisterVerifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Email address
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * 6-digit verification code
     */
    @NotBlank(message = "Verification code cannot be blank")
    @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
    private String code;
}

