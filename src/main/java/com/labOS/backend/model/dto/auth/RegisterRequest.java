package com.labOS.backend.model.dto.auth;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Register request
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
public class RegisterRequest implements Serializable {

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
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * Confirm password
     */
    @NotBlank(message = "Confirm password cannot be blank")
    private String confirmPassword;

    /**
     * Verification code
     */
    @NotBlank(message = "Verification code cannot be blank")
    @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
    private String code;

    /**
     * First name
     */
    @NotBlank(message = "First name cannot be blank")
    private String firstName;

    /**
     * Last name
     */
    @NotBlank(message = "Last name cannot be blank")
    private String lastName;

    /**
     * Legal terms acceptance
     * Must be true to register
     */
    @NotNull(message = "Legal acceptance is required")
    private Boolean legalAccepted;
}

