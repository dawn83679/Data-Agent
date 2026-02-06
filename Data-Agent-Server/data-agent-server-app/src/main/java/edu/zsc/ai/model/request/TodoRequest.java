package edu.zsc.ai.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TodoRequest
 * Simplified model for AI to provide task details in a list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoRequest {

    /**
     * Task Title
     */
    private String title;

    /**
     * Task Description
     */
    private String description;

    /**
     * Task Priority: LOW, MEDIUM, HIGH
     */
    private String priority;

    /**
     * Task Status: NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED
     */
    private String status;
}
