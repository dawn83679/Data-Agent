package edu.zsc.ai.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Register Request
 *
 * @author Data-Agent Team
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 20, message = "Password length must be between 6-20 characters")
    private String password;

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 2, max = 20, message = "Username length must be between 2-20 characters")
    private String username;
}
