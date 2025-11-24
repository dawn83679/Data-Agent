package edu.zsc.ai.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update User Profile Request
 *
 * @author Data-Agent Team
 */
@Data
public class UpdateUserProfileRequest {

    /**
     * Username
     */
    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
    private String username;

    /**
     * Avatar URL
     */
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatar;

    /**
     * Phone number
     */
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;
}
