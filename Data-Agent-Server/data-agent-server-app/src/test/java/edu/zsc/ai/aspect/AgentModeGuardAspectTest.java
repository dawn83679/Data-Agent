package edu.zsc.ai.aspect;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.annotation.DisallowInPlanMode;
import edu.zsc.ai.agent.tool.error.ToolErrorMapper;
import edu.zsc.ai.agent.tool.sql.model.AgentSqlResult;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentModeGuardAspectTest {

    @AfterEach
    void tearDown() {
        AgentRequestContext.clear();
        RequestContext.clear();
    }

    @Test
    void disallowInPlanMode_blocksExecutionInPlanMode() {
        GuardedService proxy = proxy(new GuardedService());

        AgentSqlResult result = proxy.guardedCall(InvocationParameters.from(Map.of(
                InvocationContextConstant.AGENT_MODE, "plan"
        )));
        assertFalse(result.isSuccess());
        assertEquals(
                "executeSelectSql is disabled in Plan mode — execution tools cannot run during planning. "
                        + "Keep the SQL in the planning response instead of executing it.",
                result.getError()
        );
    }

    @Test
    void disallowInPlanMode_allowsExecutionOutsidePlanMode() {
        GuardedService proxy = proxy(new GuardedService());

        AgentSqlResult result = proxy.guardedCall(InvocationParameters.from(Map.of(
                InvocationContextConstant.AGENT_MODE, "agent"
        )));
        assertTrue(result.isSuccess());
        assertEquals("RESULT_SET", result.getType());
    }

    private static GuardedService proxy(GuardedService target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new AgentToolContextAspect(new ToolErrorMapper()));
        factory.addAspect(new AgentModeGuardAspect());
        return factory.getProxy();
    }

    @AgentTool
    static class GuardedService {

        @DisallowInPlanMode(ToolNameEnum.EXECUTE_SELECT_SQL)
        AgentSqlResult guardedCall(InvocationParameters parameters) {
            return AgentSqlResult.builder()
                    .success(true)
                    .type("RESULT_SET")
                    .build();
        }
    }
}
