package edu.zsc.ai.domain.model.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for ai_message_block table
 *
 * @author AI System
 * @since 1.0.0
 */
@Data
@TableName("ai_message_block")
public class AiMessageBlock {

    /**
     * Primary key ID for message block
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Belonged message ID
     */
    @TableField("message_id")
    private Long messageId;

    /**
     * Block type, text: text tool_call: tool call tool_result: tool result
     */
    @TableField("block_type")
    private String blockType;

    /**
     * Block content
     */
    @TableField("content")
    private String content;

    /**
     * Extension data in JSON format for additional information, such as tool name, parameters, etc.
     */
    @TableField("extension_data")
    private String extensionData;

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