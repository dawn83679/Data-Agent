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

    private String scope;

    private String memoryType;

    private String subType;

    private String sourceType;

    private String title;

    private String content;

    private String reason;

    /**
     * SMALLINT: 1=ENABLE, 0=DISABLE
     */
    private Integer enable;

    private Integer accessCount;

    private LocalDateTime lastAccessedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
