package edu.zsc.ai.aspect;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.error.ToolErrorMapper;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.sql.model.AgentSqlResult;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolContextAspectTest {

    @AfterEach
    void tearDown() {
        AgentRequestContext.clear();
        RequestContext.clear();
    }

    @Test
    void mapsTypedExceptionToAgentToolResult() {
        TestTool proxy = proxy(new TestTool());

        AgentToolResult result = proxy.failTool(InvocationParameters.from(Map.of(
                InvocationContextConstant.CONNECTION_ID, "5"
        )));

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("bad input"));
        assertNotNull(result.getElapsedMs());
    }

    @Test
    void mapsUnexpectedExceptionToAgentSqlResult() {
        TestTool proxy = proxy(new TestTool());

        AgentSqlResult result = proxy.failSql(InvocationParameters.from(Map.of(
                InvocationContextConstant.CONNECTION_ID, "5"
        )));

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("failed unexpectedly"));
    }

    @Test
    void mapsTypedExceptionToImmediatePayload() {
        TestTool proxy = proxy(new TestTool());

        AskUserConfirmTool.WriteConfirmationResult result = proxy.failConfirmation(InvocationParameters.from(Map.of(
                InvocationContextConstant.CONNECTION_ID, "5"
        )));

        assertTrue(result.isError());
        assertTrue(result.getErrorMessage().contains("confirmation failed"));
    }

    private static TestTool proxy(TestTool target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new AgentToolContextAspect(new ToolErrorMapper()));
        return factory.getProxy();
    }

    @AgentTool
    static class TestTool {

        AgentToolResult failTool(InvocationParameters parameters) {
            throw AgentToolExecuteException.invalidInput(ToolNameEnum.SEARCH_OBJECTS, "bad input");
        }

        AgentSqlResult failSql(InvocationParameters parameters) {
            throw new RuntimeException("db down");
        }

        AskUserConfirmTool.WriteConfirmationResult failConfirmation(InvocationParameters parameters) {
            throw AgentToolExecuteException.preconditionFailed(ToolNameEnum.ASK_USER_CONFIRM, "confirmation failed");
        }
    }
}
