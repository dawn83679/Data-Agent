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
     * Database Name
     */
    private String databaseName;
    
    /**
     * Schema Name
     */
    private String schemaName;
}
