package edu.zsc.ai.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request Context Information
 * Contains current request context data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestContextInfo {

    /**
     * Conversation ID
     */
    private Long conversationId;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Connection ID
     */
    private Long connectionId;

    /**
     * Database catalog name
     */
    private String catalog;

    /**
     * Schema name
     */
    private String schema;

    /**
     * Resolved model name for the current agent run
     */
    private String modelName;

    /**
     * Request language used to resolve prompts
     */
    private String language;

    /**
     * Agent mode (agent / plan)
     */
    private String agentMode;

    /**
     * Multi-agent run id
     */
    private Long runId;

    /**
     * Multi-agent task id
     */
    private Long taskId;

    /**
     * Current agent role
     */
    private String agentRole;

    /**
     * Parent agent role for delegated tasks
     */
    private String parentAgentRole;
}
