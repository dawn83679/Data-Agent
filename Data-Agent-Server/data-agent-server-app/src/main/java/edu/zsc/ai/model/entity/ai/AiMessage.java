package edu.zsc.ai.model.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for ai_message table
 *
 * @author AI System
 * @since 1.0.0
 */
@Data
@TableName("ai_message")
public class AiMessage {

    /**
     * Primary key ID for message
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Belonged conversation ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * Message role, user: user message assistant: AI assistant message
     */
    @TableField("role")
    private String role;

    /**
     * Token usage statistics
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * Message status: 0=normal, 1=invalid (manually deleted/rolled back), 2=compressed
     */
    @TableField("status")
    private Integer status;

    /**
     * Message priority: 0=normal message, 1=summary message
     */
    @TableField("priority")
    private Integer priority;

    /**
     * Created time
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * Updated time
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;
}