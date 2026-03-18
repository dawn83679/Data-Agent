package edu.zsc.ai.config.ai;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.ask.AskUserQuestionTool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.orchestrator.CallingExplorerTool;
import edu.zsc.ai.agent.tool.orchestrator.CallingPlannerTool;
import edu.zsc.ai.agent.tool.plan.EnterPlanModeTool;
import edu.zsc.ai.agent.tool.plan.ExitPlanModeTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.agent.tool.sql.GetEnvironmentOverviewTool;
import edu.zsc.ai.agent.tool.sql.GetObjectDetailTool;
import edu.zsc.ai.agent.tool.sql.SearchObjectsTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentToolConfigTest {

    private AgentToolConfig config;
    private List<Object> allTools;

    // Mocked tool instances — mock() creates real subclass instances so getClass() matches
    private GetEnvironmentOverviewTool getEnvironmentOverviewTool;
    private SearchObjectsTool searchObjectsTool;
    private GetObjectDetailTool getObjectDetailTool;
    private ExecuteSqlTool executeSqlTool;
    private AskUserConfirmTool askUserConfirmTool;
    private AskUserQuestionTool askUserQuestionTool;
    private CallingExplorerTool callingExplorerTool;
    private CallingPlannerTool callingPlannerTool;
    private TodoTool todoTool;
    private EnterPlanModeTool enterPlanModeTool;
    private ExitPlanModeTool exitPlanModeTool;
    private ActivateSkillTool activateSkillTool;
    private ChartTool chartTool;

    @BeforeEach
    void setUp() {
        config = new AgentToolConfig();

        getEnvironmentOverviewTool = mock(GetEnvironmentOverviewTool.class);
        searchObjectsTool = mock(SearchObjectsTool.class);
        getObjectDetailTool = mock(GetObjectDetailTool.class);
        executeSqlTool = mock(ExecuteSqlTool.class);
        askUserConfirmTool = mock(AskUserConfirmTool.class);
        askUserQuestionTool = mock(AskUserQuestionTool.class);
        callingExplorerTool = mock(CallingExplorerTool.class);
        callingPlannerTool = mock(CallingPlannerTool.class);
        todoTool = mock(TodoTool.class);
        enterPlanModeTool = mock(EnterPlanModeTool.class);
        exitPlanModeTool = mock(ExitPlanModeTool.class);
        activateSkillTool = mock(ActivateSkillTool.class);
        chartTool = mock(ChartTool.class);

        allTools = List.of(
                getEnvironmentOverviewTool,
                searchObjectsTool,
                getObjectDetailTool,
                executeSqlTool,
                askUserConfirmTool,
                askUserQuestionTool,
                callingExplorerTool,
                callingPlannerTool,
                todoTool,
                enterPlanModeTool,
                exitPlanModeTool,
                activateSkillTool,
                chartTool
        );
    }

    @Nested
    class MainToolMatrix {

        @Test
        void agentMode_exposesOnlyExecutionFacingMainTools() {
            List<Object> tools = config.resolveMainTools(allTools, AgentModeEnum.AGENT);

            assertTrue(tools.contains(getEnvironmentOverviewTool));
            assertTrue(tools.contains(executeSqlTool));
            assertTrue(tools.contains(askUserConfirmTool));
            assertTrue(tools.contains(askUserQuestionTool));
            assertTrue(tools.contains(callingExplorerTool));
            assertTrue(tools.contains(callingPlannerTool));
            assertTrue(tools.contains(todoTool));
            assertTrue(tools.contains(activateSkillTool));
            assertTrue(tools.contains(chartTool));

            assertFalse(tools.contains(searchObjectsTool));
            assertFalse(tools.contains(getObjectDetailTool));
            assertFalse(tools.contains(enterPlanModeTool));
            assertFalse(tools.contains(exitPlanModeTool));
        }

        @Test
        void planMode_exposesOnlyPlanningFacingMainTools() {
            List<Object> tools = config.resolveMainTools(allTools, AgentModeEnum.PLAN);

            assertTrue(tools.contains(getEnvironmentOverviewTool));
            assertTrue(tools.contains(askUserQuestionTool));
            assertTrue(tools.contains(callingExplorerTool));
            assertTrue(tools.contains(callingPlannerTool));
            assertTrue(tools.contains(todoTool));
            assertTrue(tools.contains(activateSkillTool));

            assertFalse(tools.contains(executeSqlTool));
            assertFalse(tools.contains(askUserConfirmTool));
            assertFalse(tools.contains(chartTool));
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

            assertEquals(3, tools.size(), "Explorer should have 3 scoped discovery tools");
            assertFalse(tools.contains(getEnvironmentOverviewTool), "Explorer should NOT have GetEnvironmentOverviewTool");
            assertTrue(tools.contains(todoTool), "Explorer should have TodoTool");
            assertTrue(tools.contains(searchObjectsTool), "Explorer should have SearchObjectsTool");
            assertTrue(tools.contains(getObjectDetailTool), "Explorer should have GetObjectDetailTool");
        }

        @Test
        void planner_hasTodoSkillAndObjectDetail() {
            List<Object> tools = config.resolveSubAgentTools(allTools, AgentTypeEnum.PLANNER);

            assertEquals(3, tools.size(), "Planner should have exactly 3 tools");
            assertTrue(tools.contains(todoTool), "Planner should have TodoTool");
            assertTrue(tools.contains(activateSkillTool), "Planner should have ActivateSkillTool");
            assertTrue(tools.contains(getObjectDetailTool), "Planner should have GetObjectDetailTool");
        }

        @Test
        void planner_excludesExecutionAndDiscoveryTools() {
            List<Object> tools = config.resolveSubAgentTools(allTools, AgentTypeEnum.PLANNER);

            assertFalse(tools.contains(executeSqlTool), "Planner should NOT have ExecuteSqlTool");
            assertFalse(tools.contains(getEnvironmentOverviewTool), "Planner should NOT have GetEnvironmentOverviewTool");
            assertFalse(tools.contains(searchObjectsTool), "Planner should NOT have SearchObjectsTool");
            assertFalse(tools.contains(askUserConfirmTool), "Planner should NOT have AskUserConfirmTool");
            assertFalse(tools.contains(askUserQuestionTool), "Planner should NOT have AskUserQuestionTool");
            assertFalse(tools.contains(callingExplorerTool), "Planner should NOT have CallingExplorerTool");
            assertFalse(tools.contains(callingPlannerTool), "Planner should NOT have CallingPlannerTool");
            assertFalse(tools.contains(chartTool), "Planner should NOT have ChartTool");
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
    void buildToolExecutors_supportsCglibProxyAndInvokesProxyMethod() {
        AtomicInteger adviceCounter = new AtomicInteger();
        EchoTool target = new EchoTool();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice((MethodInterceptor) invocation -> {
            adviceCounter.incrementAndGet();
            return invocation.proceed();
        });
        Object proxy = proxyFactory.getProxy();

        Map<ToolSpecification, ToolExecutor> executors = config.buildToolExecutors(List.of(proxy));

        assertEquals(1, executors.size());

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
}
