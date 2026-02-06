package edu.zsc.ai.domain.model.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI Todo Task Entity
 * Maps to ai_todo_task table.
 * Stores the entire TodoList as a JSON string in the content field.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_todo_task")
public class AiTodoTask {

    /**
     * Primary key
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Associated conversation ID
     */
    private Long conversationId;

    /**
     * Associated user ID
     */
    private Long userId;

    /**
     * JSON array of task objects stored as text
     */
    private String content;

    /**
     * Created time
     */
    private LocalDateTime createdAt;

    /**
     * Updated time
     */
    private LocalDateTime updatedAt;
}
