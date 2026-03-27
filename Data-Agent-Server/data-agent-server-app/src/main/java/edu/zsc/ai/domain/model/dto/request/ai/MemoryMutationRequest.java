package edu.zsc.ai.domain.model.dto.request.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemoryMutationRequest {

    private String operation;

    private Long memoryId;

    private String scope;

    private String memoryType;

    private String subType;

    private String title;

    private String content;

    private String reason;
}
