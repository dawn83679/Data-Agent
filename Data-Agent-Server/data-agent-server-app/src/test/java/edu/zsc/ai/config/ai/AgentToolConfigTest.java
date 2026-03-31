package edu.zsc.ai.config.ai;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.AskUserQuestionTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.export.ExportFileTool;
import edu.zsc.ai.agent.tool.memory.ReadMemoryTool;
import edu.zsc.ai.agent.tool.memory.UpdateMemoryTool;
import edu.zsc.ai.agent.tool.orchestrator.CallingExplorerTool;
import edu.zsc.ai.agent.tool.orchestrator.CallingPlannerTool;
import edu.zsc.ai.agent.tool.plan.EnterPlanModeTool;
import edu.zsc.ai.agent.tool.plan.ExitPlanModeTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.sql.GetDatabasesTool;
import edu.zsc.ai.agent.tool.sql.GetObjectDetailTool;
import edu.zsc.ai.agent.tool.sql.GetSchemasTool;
import edu.zsc.ai.agent.tool.sql.SearchObjectsTool;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolConfigTest {

    private AgentToolConfig config;
    private List<Object> allTools;

    private GetDatabasesTool getDatabasesTool;
    private GetSchemasTool getSchemasTool;
    private SearchObjectsTool searchObjectsTool;
    private GetObjectDetailTool getObjectDetailTool;
    private ExecuteSqlTool executeSqlTool;
    private AskUserQuestionTool askUserQuestionTool;
    private CallingExplorerTool callingExplorerTool;
    private CallingPlannerTool callingPlannerTool;
    private TodoTool todoTool;
    private EnterPlanModeTool enterPlanModeTool;
    private ExitPlanModeTool exitPlanModeTool;
    private ActivateSkillTool activateSkillTool;
    private ChartTool chartTool;
    private ReadMemoryTool readMemoryTool;
    private UpdateMemoryTool updateMemoryTool;
    private ExportFileTool exportFileTool;

    @BeforeEach
    void setUp() {
        config = new AgentToolConfig();

        getDatabasesTool = new GetDatabasesTool(null);
        getSchemasTool = new GetSchemasTool(null);
        searchObjectsTool = new SearchObjectsTool(null);
        getObjectDetailTool = new GetObjectDetailTool(null);
        executeSqlTool = new ExecuteSqlTool(null, null, null);
        askUserQuestionTool = new AskUserQuestionTool();
        callingExplorerTool = new CallingExplorerTool(null, null, null);
        callingPlannerTool = new CallingPlannerTool(null, null, null);
        todoTool = new TodoTool();
        enterPlanModeTool = new EnterPlanModeTool();
        exitPlanModeTool = new ExitPlanModeTool();
        activateSkillTool = new ActivateSkillTool(new AgentSkillConfig());
        chartTool = new ChartTool();
        readMemoryTool = new ReadMemoryTool(null, null);
        updateMemoryTool = new UpdateMemoryTool(null);
        exportFileTool = new ExportFileTool(null);

        allTools = List.of(
                getDatabasesTool,
                getSchemasTool,
                searchObjectsTool,
                getObjectDetailTool,
                executeSqlTool,
                askUserQuestionTool,
                callingExplorerTool,
                callingPlannerTool,
                todoTool,
                enterPlanModeTool,
                exitPlanModeTool,
                activateSkillTool,
                chartTool,
                readMemoryTool,
                updateMemoryTool,
                exportFileTool
        );
    }

    @Nested
    class MainToolMatrix {

        @Test
        void agentMode_exposesOnlyExecutionFacingMainTools() {
            List<Object> tools = config.resolveMainTools(allTools, AgentModeEnum.AGENT);

            assertTrue(tools.contains(getDatabasesTool));
            assertTrue(tools.contains(getSchemasTool));
            assertTrue(tools.contains(searchObjectsTool));
            assertTrue(tools.contains(getObjectDetailTool));
            assertTrue(tools.contains(executeSqlTool));
            assertTrue(tools.contains(askUserQuestionTool));
            assertTrue(tools.contains(callingExplorerTool));
            assertTrue(tools.contains(callingPlannerTool));
            assertTrue(tools.contains(todoTool));
            assertTrue(tools.contains(activateSkillTool));
            assertTrue(tools.contains(chartTool));
            assertTrue(tools.contains(exportFileTool));
            assertFalse(tools.contains(readMemoryTool));
            assertFalse(tools.contains(updateMemoryTool));

            assertFalse(tools.contains(enterPlanModeTool));
            assertFalse(tools.contains(exitPlanModeTool));
        }

        @Test
        void planMode_exposesOnlyPlanningFacingMainTools() {
            List<Object> tools = config.resolveMainTools(allTools, AgentModeEnum.PLAN);

            assertTrue(tools.contains(getDatabasesTool));
            assertTrue(tools.contains(getSchemasTool));
            assertTrue(tools.contains(askUserQuestionTool));
            assertTrue(tools.contains(callingExplorerTool));
            assertTrue(tools.contains(callingPlannerTool));
            assertTrue(tools.contains(todoTool));

            assertFalse(tools.contains(executeSqlTool));
            assertFalse(tools.contains(activateSkillTool));
            assertFalse(tools.contains(chartTool));
            assertFalse(tools.contains(readMemoryTool));
            assertFalse(tools.contains(updateMemoryTool));
            assertFalse(tools.contains(exportFileTool));
            assertFalse(tools.contains(searchObjectsTool));
            assertFalse(tools.contains(getObjectDetailTool));
            assertFalse(tools.contains(enterPlanModeTool));
            assertFalse(tools.contains(exitPlanModeTool));
        }
    }

    @Nested
    class SubAgentToolMatrix {

        @Test
        void explorer_hasDiscoveryTools() {
            List<Object> tools = config.resolveSubAgentTools(allTools, AgentTypeEnum.EXPLORER);

            assertEquals(4, tools.size(), "Explorer should have 4 scoped tools");
            assertFalse(tools.contains(getDatabasesTool), "Explorer should NOT have GetDatabasesTool");
            assertFalse(tools.contains(getSchemasTool), "Explorer should NOT have GetSchemasTool");
            assertTrue(tools.contains(todoTool), "Explorer should have TodoTool");
            assertTrue(tools.contains(searchObjectsTool), "Explorer should have SearchObjectsTool");
            assertTrue(tools.contains(getObjectDetailTool), "Explorer should have GetObjectDetailTool");
            assertTrue(tools.contains(executeSqlTool), "Explorer should have ExecuteSqlTool");
        }

        @Test
        void planner_hasTodoObjectDetailAndExecuteSql() {
            List<Object> tools = config.resolveSubAgentTools(allTools, AgentTypeEnum.PLANNER);

            assertEquals(3, tools.size(), "Planner should have exactly 3 tools");
            assertTrue(tools.contains(todoTool), "Planner should have TodoTool");
            assertTrue(tools.contains(getObjectDetailTool), "Planner should have GetObjectDetailTool");
            assertTrue(tools.contains(executeSqlTool), "Planner should have ExecuteSqlTool");
        }

        @Test
        void planner_excludesBroadDiscoveryAndOrchestrationTools() {
            List<Object> tools = config.resolveSubAgentTools(allTools, AgentTypeEnum.PLANNER);

            assertFalse(tools.contains(getDatabasesTool), "Planner should NOT have GetDatabasesTool");
            assertFalse(tools.contains(getSchemasTool), "Planner should NOT have GetSchemasTool");
            assertFalse(tools.contains(searchObjectsTool), "Planner should NOT have SearchObjectsTool");
            assertFalse(tools.contains(askUserQuestionTool), "Planner should NOT have AskUserQuestionTool");
            assertFalse(tools.contains(callingExplorerTool), "Planner should NOT have CallingExplorerTool");
            assertFalse(tools.contains(callingPlannerTool), "Planner should NOT have CallingPlannerTool");
            assertFalse(tools.contains(activateSkillTool), "Planner should NOT have ActivateSkillTool");
            assertFalse(tools.contains(chartTool), "Planner should NOT have ChartTool");
            assertFalse(tools.contains(readMemoryTool), "Planner should NOT have ReadMemoryTool");
            assertFalse(tools.contains(updateMemoryTool), "Planner should NOT have UpdateMemoryTool");
            assertFalse(tools.contains(enterPlanModeTool), "Planner should NOT have EnterPlanModeTool");
            assertFalse(tools.contains(exitPlanModeTool), "Planner should NOT have ExitPlanModeTool");
        }

        @Test
        void mainAgent_cannotBeResolvedThroughSubAgentApi() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> config.resolveSubAgentTools(allTools, AgentTypeEnum.MAIN)
            );

            assertEquals("MAIN is not a sub-agent", exception.getMessage());
        }
    }

    @Test
    void buildToolBundle_collectsImmediateReturnToolNames() {
        AgentToolConfig.ToolBundle toolBundle = config.buildToolBundle(List.of(
                new ImmediateEchoTool(),
                new EchoTool()
        ));

        assertEquals(Set.of("immediateEcho"), toolBundle.immediateReturnToolNames());
        assertEquals(2, toolBundle.executors().size());
    }

    @Test
    void buildToolBundle_supportsCglibProxyAndInvokesProxyMethod() {
        AtomicInteger adviceCounter = new AtomicInteger();
        EchoTool target = new EchoTool();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice((MethodInterceptor) invocation -> {
            adviceCounter.incrementAndGet();
            return invocation.proceed();
        });
        Object proxy = proxyFactory.getProxy();

        AgentToolConfig.ToolBundle toolBundle = config.buildToolBundle(List.of(proxy));
        Map<ToolSpecification, ToolExecutor> executors = toolBundle.executors();

        assertEquals(1, executors.size());
        assertTrue(toolBundle.immediateReturnToolNames().isEmpty());

        Map.Entry<ToolSpecification, ToolExecutor> registration = executors.entrySet().iterator().next();
        assertEquals("echo", registration.getKey().name());

        String result = registration.getValue().execute(
                ToolExecutionRequest.builder()
                        .name("echo")
                        .arguments("{\"value\":\"hello\"}")
                        .build(),
                null
        );

        assertEquals("echo:hello", result);
        assertEquals(1, adviceCounter.get(), "tool execution should still go through proxy advice");
    }

    @AgentTool
    static class EchoTool {

        @Tool
        public String echo(@P("Echo value") String value) {
            return "echo:" + value;
        }
    }

    @AgentTool
    static class ImmediateEchoTool {

        @Tool(returnBehavior = ReturnBehavior.IMMEDIATE)
        public String immediateEcho(@P("Echo value") String value) {
            return "immediate:" + value;
        }
    }
}
