package edu.zsc.ai.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base Request
 * Contains context information for requests
 * Note: userId is obtained from Sa-Token, not from request body
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseRequest {
    
    /**
     * Conversation ID
     */
    private Long conversationId;
    
    /**
     * Connection ID
     */
    private Long connectionId;
    
    /**
     * Database Name
     */
    private String databaseName;
    
    /**
     * Schema Name
     */
    private String schemaName;
}
