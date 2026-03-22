package edu.zsc.ai.domain.model.entity.ai;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("ai_memory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMemory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long conversationId;

    private String workspaceContextKey;

    private String workspaceLevel;

    private String scope;

    private String memoryType;

    private String subType;

    private String sourceType;

    private String title;

    private String content;

    private String normalizedContentKey;

    private String reason;

    private String sourceMessageIds;

    private String detailJson;

    /**
     * SMALLINT: 0=ACTIVE, 1=ARCHIVED, 2=HIDDEN
     */
    private Integer status;

    private Double confidenceScore;

    private Double salienceScore;

    private Integer accessCount;

    private Integer useCount;

    private LocalDateTime lastAccessedAt;

    private LocalDateTime lastUsedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime archivedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
