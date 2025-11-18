package edu.zsc.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity for ai_todo_task table
 *
 * @author zgq
 */
@Data
@TableName("ai_todo_task")
public class AiTodoTask {

    /**
     * Primary key ID for task
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Associated conversation ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * JSON array of task objects stored as text
     */
    @TableField("content")
    private String content;

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