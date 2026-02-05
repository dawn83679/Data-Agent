package edu.zsc.ai.context;

import lombok.extern.slf4j.Slf4j;

/**
 * Request Context Manager
 * Thread-local storage for request context
 */
@Slf4j
public class RequestContext {
    
    private static final ThreadLocal<RequestContextInfo> CONTEXT_HOLDER = new ThreadLocal<>();
    
    /**
     * Set context for current thread
     */
    public static void set(RequestContextInfo contextInfo) {
        if (contextInfo == null) {
            log.warn("Attempting to set null context");
            return;
        }
        CONTEXT_HOLDER.set(contextInfo);
        log.debug("Context set for conversation: {}, user: {}", 
            contextInfo.getConversationId(), contextInfo.getUserId());
    }
    
    /**
     * Get context from current thread
     */
    public static RequestContextInfo get() {
        RequestContextInfo context = CONTEXT_HOLDER.get();
        if (context == null) {
            log.warn("No context found in current thread");
        }
        return context;
    }
    
    /**
     * Get conversation ID from current context
     */
    public static Long getConversationId() {
        RequestContextInfo context = get();
        return context != null ? context.getConversationId() : null;
    }
    
    /**
     * Get user ID from current context
     */
    public static Long getUserId() {
        RequestContextInfo context = get();
        return context != null ? context.getUserId() : null;
    }
    
    /**
     * Get connection ID from current context
     */
    public static Long getConnectionId() {
        RequestContextInfo context = get();
        return context != null ? context.getConnectionId() : null;
    }
    
    /**
     * Get database name from current context
     */
    public static String getDatabaseName() {
        RequestContextInfo context = get();
        return context != null ? context.getDatabaseName() : null;
    }
    
    /**
     * Get schema name from current context
     */
    public static String getSchemaName() {
        RequestContextInfo context = get();
        return context != null ? context.getSchemaName() : null;
    }
    
    /**
     * Clear context from current thread
     */
    public static void clear() {
        RequestContextInfo context = CONTEXT_HOLDER.get();
        if (context != null) {
            log.debug("Clearing context for conversation: {}", context.getConversationId());
        }
        CONTEXT_HOLDER.remove();
    }
    
    /**
     * Check if context exists
     */
    public static boolean hasContext() {
        return CONTEXT_HOLDER.get() != null;
    }
}
