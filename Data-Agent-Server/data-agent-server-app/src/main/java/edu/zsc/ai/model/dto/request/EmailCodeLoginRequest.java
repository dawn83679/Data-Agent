package edu.zsc.ai.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Email Code Login Request
 *
 * @author Data-Agent Team
 */
@Data
public class EmailCodeLoginRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Verification code cannot be empty")
    private String code;

    @Override
    public String toString() {
        return "EmailCodeLoginRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}
