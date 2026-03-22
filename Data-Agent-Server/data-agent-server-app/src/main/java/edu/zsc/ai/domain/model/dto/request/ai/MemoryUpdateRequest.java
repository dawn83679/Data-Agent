package edu.zsc.ai.domain.model.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemoryUpdateRequest {

    @NotBlank(message = "memoryType is required")
    private String memoryType;

    @Size(max = 32, message = "scope must not exceed 32 characters")
    private String scope;

    @Size(max = 64, message = "subType must not exceed 64 characters")
    private String subType;

    @Size(max = 32, message = "sourceType must not exceed 32 characters")
    private String sourceType;

    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @Size(max = 512, message = "reason must not exceed 512 characters")
    private String reason;

    @NotBlank(message = "content is required")
    @Size(max = 8000, message = "content must not exceed 8000 characters")
    private String content;
}
