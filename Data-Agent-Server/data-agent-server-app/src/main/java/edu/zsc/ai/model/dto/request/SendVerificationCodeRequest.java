package edu.zsc.ai.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Send Verification Code Request
 *
 * @author Data-Agent Team
 */
@Data
public class SendVerificationCodeRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Code type cannot be empty")
    private String codeType;
}
