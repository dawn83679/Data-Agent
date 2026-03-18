package edu.zsc.ai.context;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.util.ConnectionIdUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class AgentInvocationContext implements AutoCloseable {

    private final long startTime = System.currentTimeMillis();
    private final RequestContextInfo previousRequestContext;
    private final AgentRequestContextInfo previousAgentRequestContext;

    private AgentInvocationContext(RequestContextInfo previousRequestContext,
                                   AgentRequestContextInfo previousAgentRequestContext) {
        this.previousRequestContext = previousRequestContext;
        this.previousAgentRequestContext = previousAgentRequestContext;
    }

    public static AgentInvocationContext from(InvocationParameters params) {
        RequestContextInfo previousRequestContext = RequestContext.snapshot();
        AgentRequestContextInfo previousAgentRequestContext = AgentRequestContext.snapshot();

        if (params != null) {
            RequestContextInfo requestContextInfo = RequestContextInfo.builder()
                    .userId(ConnectionIdUtil.toLong(params.get(InvocationContextConstant.USER_ID)))
                    .conversationId(ConnectionIdUtil.toLong(params.get(InvocationContextConstant.CONVERSATION_ID)))
                    .connectionId(ConnectionIdUtil.toLong(params.get(InvocationContextConstant.CONNECTION_ID)))
                    .catalog(params.get(InvocationContextConstant.DATABASE_NAME))
                    .schema(params.get(InvocationContextConstant.SCHEMA_NAME))
                    .build();
            AgentRequestContextInfo agentRequestContextInfo = AgentRequestContextInfo.builder()
                    .agentMode(params.get(InvocationContextConstant.AGENT_MODE))
                    .agentType(params.get(InvocationContextConstant.AGENT_TYPE))
                    .allowedConnectionIds(ConnectionIdUtil.toLongList(params.get(InvocationContextConstant.ALLOWED_CONNECTION_IDS)))
                    .modelName(params.get(InvocationContextConstant.MODEL_NAME))
                    .build();

            if (hasRequestContextData(requestContextInfo)) {
                RequestContext.set(requestContextInfo);
            } else {
                RequestContext.clear();
            }
            if (hasAgentRequestContextData(agentRequestContextInfo)) {
                AgentRequestContext.set(agentRequestContextInfo);
            } else {
                AgentRequestContext.clear();
            }
        }

        return new AgentInvocationContext(previousRequestContext, previousAgentRequestContext);
    }

    public AgentToolResult timed(AgentToolResult result) {
        result.setElapsedMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public void close() {
        if (previousRequestContext != null) {
            RequestContext.set(previousRequestContext);
        } else {
            RequestContext.clear();
        }

        if (previousAgentRequestContext != null) {
            AgentRequestContext.set(previousAgentRequestContext);
        } else {
            AgentRequestContext.clear();
        }
    }

    private static boolean hasRequestContextData(RequestContextInfo contextInfo) {
        return contextInfo.getUserId() != null
                || contextInfo.getConversationId() != null
                || contextInfo.getConnectionId() != null
                || StringUtils.isNotBlank(contextInfo.getCatalog())
                || StringUtils.isNotBlank(contextInfo.getSchema());
    }

    private static boolean hasAgentRequestContextData(AgentRequestContextInfo contextInfo) {
        return StringUtils.isNotBlank(contextInfo.getAgentMode())
                || StringUtils.isNotBlank(contextInfo.getAgentType())
                || StringUtils.isNotBlank(contextInfo.getModelName())
                || CollectionUtils.isNotEmpty(contextInfo.getAllowedConnectionIds());
    }
}
