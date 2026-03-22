package edu.zsc.ai.context;

import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.util.ConnectionIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class AgentRequestContext {

    private static final ThreadLocal<AgentRequestContextInfo> CONTEXT_HOLDER = new ThreadLocal<>();

    private AgentRequestContext() {
    }

    public static void set(AgentRequestContextInfo contextInfo) {
        if (contextInfo == null) {
            log.warn("Attempting to set null agent context");
            return;
        }
        CONTEXT_HOLDER.set(contextInfo);
        log.debug("Agent context set: agentType={}, agentMode={}",
                contextInfo.getAgentType(), contextInfo.getAgentMode());
    }

    public static AgentRequestContextInfo get() {
        AgentRequestContextInfo context = peek();
        if (context == null) {
            log.warn("No agent context found in current thread");
        }
        return context;
    }

    public static AgentRequestContextInfo snapshot() {
        AgentRequestContextInfo context = peek();
        return context == null ? null : context.toBuilder().build();
    }

    public static boolean hasContext() {
        return peek() != null;
    }

    public static void clear() {
        AgentRequestContextInfo context = peek();
        if (context != null) {
            log.debug("Clearing agent context: agentType={}, agentMode={}",
                    context.getAgentType(), context.getAgentMode());
        }
        CONTEXT_HOLDER.remove();
    }

    public static String getAgentMode() {
        AgentRequestContextInfo context = peek();
        return context != null ? context.getAgentMode() : null;
    }

    public static String getAgentType() {
        AgentRequestContextInfo context = peek();
        return context != null ? context.getAgentType() : null;
    }

    public static List<Long> getAllowedConnectionIds() {
        AgentRequestContextInfo context = peek();
        if (context == null || CollectionUtils.isEmpty(context.getAllowedConnectionIds())) {
            return List.of();
        }
        return context.getAllowedConnectionIds();
    }

    public static String getModelName() {
        AgentRequestContextInfo context = peek();
        return context != null ? context.getModelName() : null;
    }

    public static String getLanguage() {
        AgentRequestContextInfo context = peek();
        return context != null ? context.getLanguage() : null;
    }

    public static boolean isExplorerScope() {
        return StringUtils.equalsIgnoreCase(AgentTypeEnum.EXPLORER.getCode(), getAgentType());
    }

    public static List<Long> requireAllowedConnectionIds() {
        List<Long> allowedConnectionIds = getAllowedConnectionIds();
        if (CollectionUtils.isEmpty(allowedConnectionIds)) {
            throw new IllegalStateException("Explorer agent context is missing allowedConnectionIds.");
        }
        return allowedConnectionIds;
    }

    public static Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        putIfNotNull(map, InvocationContextConstant.AGENT_MODE, getAgentMode());
        putIfNotNull(map, InvocationContextConstant.AGENT_TYPE, getAgentType());
        putIfNotNull(map, InvocationContextConstant.ALLOWED_CONNECTION_IDS, ConnectionIdUtil.toCsv(getAllowedConnectionIds()));
        putIfNotNull(map, InvocationContextConstant.MODEL_NAME, getModelName());
        putIfNotNull(map, InvocationContextConstant.LANGUAGE, getLanguage());
        return map;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private static AgentRequestContextInfo peek() {
        return CONTEXT_HOLDER.get();
    }
}
