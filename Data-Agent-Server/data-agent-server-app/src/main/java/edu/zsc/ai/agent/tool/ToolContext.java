package edu.zsc.ai.agent.tool;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;

/**
 * AutoCloseable bridge: InvocationParameters → RequestContext, with built-in timing.
 *
 * Usage in @Tool methods:
 * <pre>
 * try (var ctx = ToolContext.from(parameters)) {
 *     // RequestContext is available here
 *     return ctx.timed(result);  // sets elapsedMs on the result
 * }
 * </pre>
 */
public class ToolContext implements AutoCloseable {

    private final long startTime = System.currentTimeMillis();

    public static ToolContext from(InvocationParameters params) {
        if (params != null) {
            RequestContextInfo contextInfo = RequestContextInfo.builder()
                    .userId(params.get(RequestContextConstant.USER_ID))
                    .conversationId(params.get(RequestContextConstant.CONVERSATION_ID))
                    .connectionId(params.get(RequestContextConstant.CONNECTION_ID))
                    .catalog(params.get(RequestContextConstant.DATABASE_NAME))
                    .schema(params.get(RequestContextConstant.SCHEMA_NAME))
                    .modelName(params.get(RequestContextConstant.MODEL_NAME))
                    .language(params.get(RequestContextConstant.LANGUAGE))
                    .agentMode(params.get(RequestContextConstant.AGENT_MODE))
                    .runId(params.get(RequestContextConstant.RUN_ID))
                    .taskId(params.get(RequestContextConstant.TASK_ID))
                    .agentRole(params.get(RequestContextConstant.AGENT_ROLE))
                    .parentAgentRole(params.get(RequestContextConstant.PARENT_AGENT_ROLE))
                    .build();
            RequestContext.set(contextInfo);
        }
        return new ToolContext();
    }

    /**
     * Sets elapsedMs on the result and returns it.
     */
    public AgentToolResult timed(AgentToolResult result) {
        result.setElapsedMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public void close() {
        RequestContext.clear();
    }
}
