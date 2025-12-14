package edu.zsc.ai.domain.model.dto.request.sys;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "password length must be at least 8")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password must contain letters and numbers")
    private String oldPassword;

    @NotBlank
    @Size(min = 8, message = "password length must be at least 8")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password must contain letters and numbers")
    private String newPassword;
}
