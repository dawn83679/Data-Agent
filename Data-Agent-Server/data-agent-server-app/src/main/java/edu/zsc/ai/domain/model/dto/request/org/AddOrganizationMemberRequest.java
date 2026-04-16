package edu.zsc.ai.domain.model.dto.request.org;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddOrganizationMemberRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 32)
    private String roleCode;
}
