package edu.zsc.ai.domain.model.entity.ai;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("ai_conversation_memory_cursor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationMemoryCursor {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long conversationId;

    private Long lastProcessedMessageId;

    private LocalDateTime lastProcessedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
