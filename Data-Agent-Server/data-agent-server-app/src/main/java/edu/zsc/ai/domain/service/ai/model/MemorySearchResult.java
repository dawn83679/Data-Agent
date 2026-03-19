package edu.zsc.ai.domain.service.ai.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorySearchResult {

    private Long id;

    private String scope;

    private String workspaceLevel;

    private String workspaceContextKey;

    private String memoryType;

    private String subType;

    private String title;

    private String content;

    private String normalizedContentKey;

    private String reason;

    private String reviewState;

    private String sourceType;

    private double score;

    private Long conversationId;

    private LocalDateTime updatedAt;
}
