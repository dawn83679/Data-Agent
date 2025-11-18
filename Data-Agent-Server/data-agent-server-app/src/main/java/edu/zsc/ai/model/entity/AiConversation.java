package edu.zsc.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for ai_conversation table
 *
 * @author AI System
 * @since 1.0.0
 */
@Data
@TableName("ai_conversation")
public class AiConversation {

    /**
     * Primary key ID for conversation
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Associated user ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Conversation title, can be generated from first message or customized by user
     */
    @TableField("title")
    private String title;

    /**
     * Token usage statistics
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * Soft delete flag, 0: normal 1: deleted
     */
    @TableField("delete_flag")
    private Integer deleteFlag;

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