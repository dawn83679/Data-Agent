package edu.zsc.ai.domain.model.dto.request.org;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrganizationRequest {

    @NotBlank
    @Size(max = 64)
    private String orgCode;

    @NotBlank
    @Size(max = 128)
    private String orgName;
}
