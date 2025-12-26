package com.labOS.backend.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * Update user's first name and last name
 */
@Data
public class UserUpdateNameRequest implements Serializable {

    /**
     * First name
     */
    private String firstName;

    /**
     * Last name
     */
    private String lastName;

    private static final long serialVersionUID = 1L;
}


