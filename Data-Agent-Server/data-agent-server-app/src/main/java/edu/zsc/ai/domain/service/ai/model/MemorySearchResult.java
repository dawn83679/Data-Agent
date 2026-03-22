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

    private String memoryType;

    private String subType;

    private String title;

    private String content;

    private String reason;

    private String sourceType;

    private double score;

    private Integer accessCount;

    private Long conversationId;

    private LocalDateTime updatedAt;
}
