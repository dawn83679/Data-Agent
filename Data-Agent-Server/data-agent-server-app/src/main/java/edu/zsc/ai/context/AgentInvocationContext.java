package edu.zsc.ai.context;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.org.OrganizationRoleEnum;
import edu.zsc.ai.common.enums.org.WorkspaceTypeEnum;
import edu.zsc.ai.util.ConnectionIdUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

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
            Long orgIdFromParams = ConnectionIdUtil.toLong(params.get(InvocationContextConstant.ORG_ID));
            WorkspaceTypeEnum parsedWs = parseWorkspaceType(params.get(InvocationContextConstant.WORKSPACE_TYPE));
            // LangChain / map round-trip may drop workspaceType while keeping orgId; must not fall back to PERSONAL in tools.
            if (parsedWs == null && orgIdFromParams != null) {
                parsedWs = WorkspaceTypeEnum.ORGANIZATION;
            }
            RequestContextInfo.RequestContextInfoBuilder ctxBuilder = RequestContextInfo.builder()
                    .userId(ConnectionIdUtil.toLong(params.get(InvocationContextConstant.USER_ID)))
                    .conversationId(ConnectionIdUtil.toLong(params.get(InvocationContextConstant.CONVERSATION_ID)))
                    .connectionId(ConnectionIdUtil.toLong(params.get(InvocationContextConstant.CONNECTION_ID)))
                    .catalog(params.get(InvocationContextConstant.DATABASE_NAME))
                    .schema(params.get(InvocationContextConstant.SCHEMA_NAME))
                    .workspaceType(parsedWs);
            if (orgIdFromParams != null) {
                ctxBuilder.orgId(orgIdFromParams);
            }
            Long orgUserRelId = ConnectionIdUtil.toLong(params.get(InvocationContextConstant.ORG_USER_REL_ID));
            if (orgUserRelId != null) {
                ctxBuilder.orgUserRelId(orgUserRelId);
            }
            OrganizationRoleEnum orgRole = parseOrgRole(params.get(InvocationContextConstant.ORG_ROLE));
            if (orgRole != null) {
                ctxBuilder.orgRole(orgRole);
            }
            RequestContextInfo requestContextInfo = ctxBuilder.build();
            AgentRequestContextInfo agentRequestContextInfo = AgentRequestContextInfo.builder()
                    .agentMode(params.get(InvocationContextConstant.AGENT_MODE))
                    .agentType(params.get(InvocationContextConstant.AGENT_TYPE))
                    .allowedConnectionIds(ConnectionIdUtil.toLongList(params.get(InvocationContextConstant.ALLOWED_CONNECTION_IDS)))
                    .readableConnectionIds(ConnectionIdUtil.toLongList(params.get(InvocationContextConstant.READABLE_CONNECTION_IDS)))
                    .modelName(params.get(InvocationContextConstant.MODEL_NAME))
                    .language(params.get(InvocationContextConstant.LANGUAGE))
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
                || StringUtils.isNotBlank(contextInfo.getSchema())
                || contextInfo.getWorkspaceType() != null
                || contextInfo.getOrgId() != null
                || contextInfo.getOrgRole() != null
                || contextInfo.getOrgUserRelId() != null;
    }

    private static WorkspaceTypeEnum parseWorkspaceType(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof WorkspaceTypeEnum w) {
            return w;
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return WorkspaceTypeEnum.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static OrganizationRoleEnum parseOrgRole(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof OrganizationRoleEnum r) {
            return r;
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return OrganizationRoleEnum.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean hasAgentRequestContextData(AgentRequestContextInfo contextInfo) {
        return StringUtils.isNotBlank(contextInfo.getAgentMode())
                || StringUtils.isNotBlank(contextInfo.getAgentType())
                || StringUtils.isNotBlank(contextInfo.getModelName())
                || StringUtils.isNotBlank(contextInfo.getLanguage())
                || CollectionUtils.isNotEmpty(contextInfo.getAllowedConnectionIds())
                || CollectionUtils.isNotEmpty(contextInfo.getReadableConnectionIds());
    }
}
