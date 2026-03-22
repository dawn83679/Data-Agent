package edu.zsc.ai.domain.model.dto.request.ai;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemoryWriteRequest {

    private String scope;

    private String workspaceLevel;

    private Long workspaceConnectionId;

    private String workspaceCatalogName;

    private String workspaceSchemaName;

    private String memoryType;

    private String subType;

    private String title;

    private String content;

    private String reason;

    private Double confidence;

    private List<String> sourceMessageIds;
}
