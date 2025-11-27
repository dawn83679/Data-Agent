package edu.zsc.ai.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Reset Password Request
 *
 * @author Data-Agent Team
 */
@Data
public class ResetPasswordRequest {

    /**
     * User email
     */
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * Verification code
     */
    @NotBlank(message = "Verification code cannot be empty")
    private String code;

    /**
     * New password
     */
    @NotBlank(message = "Password cannot be empty")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$",
            message = "Password must be at least 8 characters with uppercase, lowercase and number")
    private String newPassword;
}
