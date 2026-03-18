package edu.zsc.ai.context;

import edu.zsc.ai.common.constant.InvocationContextConstant;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

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

    public static RequestContextInfo snapshot() {
        RequestContextInfo context = CONTEXT_HOLDER.get();
        return context == null ? null : context.toBuilder().build();
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
     * Get catalog (database) name from current context
     */
    public static String getCatalog() {
        RequestContextInfo context = get();
        return context != null ? context.getCatalog() : null;
    }

    /**
     * Get schema name from current context
     */
    public static String getSchema() {
        RequestContextInfo context = get();
        return context != null ? context.getSchema() : null;
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

    /**
     * Update conversationId in current context (e.g. after creating a new conversation in this request).
     */
    public static void updateConversationId(Long conversationId) {
        RequestContextInfo context = CONTEXT_HOLDER.get();
        if (context != null) {
            context.setConversationId(conversationId);
        }
    }

    /**
     * Build a map from current context for passing to external callers (e.g. AI Service).
     * Only non-null values are included; callers use get(key) which returns null for absent keys.
     */
    public static Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        putIfNotNull(map, InvocationContextConstant.USER_ID, getUserId());
        putIfNotNull(map, InvocationContextConstant.CONVERSATION_ID, getConversationId());
        putIfNotNull(map, InvocationContextConstant.CONNECTION_ID, getConnectionId());
        putIfNotNull(map, InvocationContextConstant.DATABASE_NAME, getCatalog());
        putIfNotNull(map, InvocationContextConstant.SCHEMA_NAME, getSchema());
        return map;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
