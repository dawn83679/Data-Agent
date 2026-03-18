package edu.zsc.ai.agent.subagent;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.subagent.explorer.ExplorerAgentService;
import edu.zsc.ai.agent.subagent.planner.PlannerAgentService;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SubAgent invocation parameter passing and @Agent interface signatures.
 */
class SubAgentInvocationTest {

    @Test
    void explorerAgentService_hasCorrectMethodSignature() throws Exception {
        var method = ExplorerAgentService.class.getMethod("explore", String.class, InvocationParameters.class);
        assertNotNull(method);
        assertEquals(2, method.getParameterCount());
        assertEquals(TokenStream.class, method.getReturnType());
    }

    @Test
    void plannerAgentService_hasCorrectMethodSignature() throws Exception {
        var method = PlannerAgentService.class.getMethod("plan", String.class, InvocationParameters.class);
        assertNotNull(method);
        assertEquals(2, method.getParameterCount());
        assertEquals(TokenStream.class, method.getReturnType());
    }

    @Test
    void invocationParameters_canBeBuiltFromRequestContext() {
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put(InvocationContextConstant.USER_ID, "42");
        contextMap.put(InvocationContextConstant.CONVERSATION_ID, "100");
        contextMap.put(InvocationContextConstant.CONNECTION_ID, "5");

        InvocationParameters params = InvocationParameters.from(contextMap);
        assertEquals("42", params.get(InvocationContextConstant.USER_ID));
        assertEquals("100", params.get(InvocationContextConstant.CONVERSATION_ID));
        assertEquals("5", params.get(InvocationContextConstant.CONNECTION_ID));
    }

    @Test
    void invocationParameters_preservesDatabaseContext() {
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put(InvocationContextConstant.CONNECTION_ID, "5");
        contextMap.put(InvocationContextConstant.DATABASE_NAME, "mydb");
        contextMap.put(InvocationContextConstant.SCHEMA_NAME, "public");

        InvocationParameters params = InvocationParameters.from(contextMap);
        assertEquals("mydb", params.get(InvocationContextConstant.DATABASE_NAME));
        assertEquals("public", params.get(InvocationContextConstant.SCHEMA_NAME));
    }

    @Test
    void invocationParameters_preservesExplorerScopeContext() {
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put(InvocationContextConstant.AGENT_TYPE, "explorer");
        contextMap.put(InvocationContextConstant.ALLOWED_CONNECTION_IDS, "5,7");

        InvocationParameters params = InvocationParameters.from(contextMap);
        assertEquals("explorer", params.get(InvocationContextConstant.AGENT_TYPE));
        assertEquals("5,7", params.get(InvocationContextConstant.ALLOWED_CONNECTION_IDS));
    }
}
