package edu.zsc.ai.domain.model.dto.request.ai;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemoryCreateRequest {

    private Long conversationId;

    @NotBlank(message = "memoryType is required")
    private String memoryType;

    @Size(max = 32, message = "scope must not exceed 32 characters")
    private String scope;

    @Size(max = 16, message = "workspaceLevel must not exceed 16 characters")
    private String workspaceLevel;

    @Size(max = 255, message = "workspaceContextKey must not exceed 255 characters")
    private String workspaceContextKey;

    @Size(max = 64, message = "subType must not exceed 64 characters")
    private String subType;

    @Size(max = 32, message = "reviewState must not exceed 32 characters")
    private String reviewState;

    @Size(max = 32, message = "sourceType must not exceed 32 characters")
    private String sourceType;

    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    @Size(max = 512, message = "reason must not exceed 512 characters")
    private String reason;

    @NotBlank(message = "content is required")
    @Size(max = 8000, message = "content must not exceed 8000 characters")
    private String content;

    @Size(max = 20000, message = "detailJson must not exceed 20000 characters")
    private String detailJson;

    @Size(max = 4000, message = "sourceMessageIds must not exceed 4000 characters")
    private String sourceMessageIds;

    @DecimalMin(value = "0.0", message = "confidenceScore must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "confidenceScore must be between 0 and 1")
    private Double confidenceScore;

    @DecimalMin(value = "0.0", message = "salienceScore must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "salienceScore must be between 0 and 1")
    private Double salienceScore;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
}
