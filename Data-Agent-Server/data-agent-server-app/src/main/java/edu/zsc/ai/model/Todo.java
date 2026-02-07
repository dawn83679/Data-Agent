package edu.zsc.ai.model;

import edu.zsc.ai.common.enums.ai.TodoPriority;
import edu.zsc.ai.common.enums.ai.TodoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Todo Item
 * Represents a single todo task
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Todo {
    
    /**
     * Todo Title
     */
    private String title;
    
    /**
     * Todo Description
     */
    private String description;
    
    /**
     * Todo Status: NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED
     */
    @Builder.Default
    private String status = TodoStatus.NOT_STARTED.name();
    
    /**
     * Todo Priority: LOW, MEDIUM, HIGH
     */
    @Builder.Default
    private String priority = TodoPriority.MEDIUM.name();
    
    /**
     * Created Time
     */
    private LocalDateTime createdAt;
    
    /**
     * Updated Time
     */
    private LocalDateTime updatedAt;
}
