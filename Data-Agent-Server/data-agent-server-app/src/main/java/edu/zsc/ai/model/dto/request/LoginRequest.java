package edu.zsc.ai.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login Request
 *
 * @author Data-Agent Team
 */
@Data
public class LoginRequest {

    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                ", rememberMe=" + rememberMe +
                '}';
    }

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    private String password;

    /**
     * Remember me flag
     */
    private Boolean rememberMe = false;
}
