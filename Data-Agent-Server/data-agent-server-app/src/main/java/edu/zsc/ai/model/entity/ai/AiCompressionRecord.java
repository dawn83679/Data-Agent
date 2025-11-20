package edu.zsc.ai.model.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for ai_compression_record table
 *
 * @author AI System
 * @since 1.0.0
 */
@Data
@TableName("ai_compression_record")
public class AiCompressionRecord {

    /**
     * Primary key ID for compression record
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Associated conversation ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * Starting message ID of compression range
     */
    @TableField("start_message_id")
    private Long startMessageId;

    /**
     * Ending message ID of compression range
     */
    @TableField("end_message_id")
    private Long endMessageId;

    /**
     * Summary message ID generated from compression
     */
    @TableField("summary_message_id")
    private Long summaryMessageId;

    /**
     * Compression strategy used (e.g., "SUMMARY", "KEY_POINTS")
     */
    @TableField("compression_strategy")
    private String compressionStrategy;

    /**
     * Total token count before compression
     */
    @TableField("token_before")
    private Integer tokenBefore;

    /**
     * Total token count after compression
     */
    @TableField("token_after")
    private Integer tokenAfter;

    /**
     * Compression status: 0=active, 1=rolled_back
     */
    @TableField("status")
    private Integer status;

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