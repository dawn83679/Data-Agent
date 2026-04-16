package edu.zsc.ai.context;

import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.org.OrganizationRoleEnum;
import edu.zsc.ai.common.enums.org.WorkspaceTypeEnum;
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

    public static WorkspaceTypeEnum getWorkspaceType() {
        RequestContextInfo context = get();
        return context != null ? context.getWorkspaceType() : null;
    }

    /**
     * Defaults to {@link WorkspaceTypeEnum#PERSONAL} when unset (e.g. legacy tests).
     */
    public static WorkspaceTypeEnum getWorkspaceTypeOrPersonal() {
        WorkspaceTypeEnum t = getWorkspaceType();
        return t != null ? t : WorkspaceTypeEnum.PERSONAL;
    }

    /** Personal workspace effective. */
    public static boolean isPersonalWorkspaceEffective() {
        WorkspaceTypeEnum ws = getWorkspaceType();
        if (ws == WorkspaceTypeEnum.PERSONAL) {
            return true;
        }
        return ws == null && getOrgId() == null;
    }

    /** Organization workspace effective. */
    public static boolean isOrganizationWorkspaceEffective() {
        WorkspaceTypeEnum ws = getWorkspaceType();
        Long orgId = getOrgId();
        if (ws == WorkspaceTypeEnum.ORGANIZATION) {
            return true;
        }
        return ws == null && orgId != null;
    }

    public static Long getOrgId() {
        RequestContextInfo context = get();
        return context != null ? context.getOrgId() : null;
    }

    public static Long getOrgUserRelId() {
        RequestContextInfo context = get();
        return context != null ? context.getOrgUserRelId() : null;
    }

    public static OrganizationRoleEnum getOrgRole() {
        RequestContextInfo context = get();
        return context != null ? context.getOrgRole() : null;
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
        WorkspaceTypeEnum ws = getWorkspaceType();
        if (ws != null) {
            map.put(InvocationContextConstant.WORKSPACE_TYPE, ws.name());
        }
        putIfNotNull(map, InvocationContextConstant.ORG_ID, getOrgId());
        putIfNotNull(map, InvocationContextConstant.ORG_USER_REL_ID, getOrgUserRelId());
        OrganizationRoleEnum role = getOrgRole();
        if (role != null) {
            map.put(InvocationContextConstant.ORG_ROLE, role.name());
        }
        return map;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
