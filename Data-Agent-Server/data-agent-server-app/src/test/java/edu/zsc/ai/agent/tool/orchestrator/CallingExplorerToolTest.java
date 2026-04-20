package edu.zsc.ai.agent.tool.orchestrator;

import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.contract.*;
import edu.zsc.ai.agent.subagent.explorer.ExplorerSubAgent;
import edu.zsc.ai.agent.subagent.planner.PlannerSubAgent;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.config.ai.SubAgentProperties;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.domain.service.db.ConnectionAccessService;
import edu.zsc.ai.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CallingExplorerToolTest {

    private ExplorerSubAgent mockExplorer;
    private CallingExplorerTool tool;
    private ExecutorService executorService;
    private Executor explorerExecutor;
    private ConnectionAccessService connectionAccessService;

    @BeforeEach
    void setUp() {
        mockExplorer = mock(ExplorerSubAgent.class);
        PlannerSubAgent mockPlanner = mock(PlannerSubAgent.class);
        SubAgentProperties properties = new SubAgentProperties();
        SubAgentManager subAgentManager = new SubAgentManager(mockExplorer, mockPlanner, properties);
        executorService = Executors.newFixedThreadPool(3);
        explorerExecutor = executorService;
        connectionAccessService = mock(ConnectionAccessService.class);
        doNothing().when(connectionAccessService).assertReadable(anyLong());
        tool = new CallingExplorerTool(subAgentManager, explorerExecutor, connectionAccessService);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    private SchemaSummary buildTestSchema(String tableName) {
        return SchemaSummary.builder()
                .summaryText("found " + tableName)
                .objects(List.of(
                        ExploreObject.builder()
                                .catalog("analytics")
                                .schema("public")
                                .objectName(tableName)
                                .objectType("TABLE")
                                .objectDdl("CREATE TABLE " + tableName + " (id int8)")
                                .relevanceScore(90)
                                .build()
                ))
                .rawResponse("explored " + tableName)
                .build();
    }

    @Test
    void singleTask_invokesExplorer() {
        when(mockExplorer.invoke(any(SubAgentRequest.class))).thenReturn(buildTestSchema("users"));

        List<ExplorerTask> tasks = List.of(new ExplorerTask(1L, "explore all tables", null, null));
        AgentToolResult result = tool.callingExplorerSubAgent(tasks, null, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Explorer results are available for 1 task(s)"));
        assertTrue(result.getMessage().contains("Use the returned summaries and objects to continue planning or direct inspection"));
        assertTrue(result.getMessage().contains("If one returned target already looks sufficient"));
        assertTrue(result.getMessage().contains("If multiple targets remain plausible"));
        ExplorerResultEnvelope envelope = JsonUtil.json2Object((String) result.getResult(), ExplorerResultEnvelope.class);
        assertEquals(1, envelope.getTaskResults().size());
        assertEquals("users", envelope.getTaskResults().get(0).getObjects().get(0).getObjectName());
        verify(mockExplorer).invoke(any(SubAgentRequest.class));
    }

    @Test
    void nullTasks_fails() {
        AgentToolExecuteException exception = assertThrows(
                AgentToolExecuteException.class,
                () -> tool.callingExplorerSubAgent(null, null, null)
        );
        assertTrue(exception.getMessageForModel().contains("tasks is required"));
    }

    @Test
    void emptyArray_fails() {
        AgentToolExecuteException exception = assertThrows(
                AgentToolExecuteException.class,
                () -> tool.callingExplorerSubAgent(List.of(), null, null)
        );
        assertTrue(exception.getMessageForModel().contains("tasks is required"));
    }

    @Test
    void invalidTask_failsFast() {
        AgentToolExecuteException exception = assertThrows(
                AgentToolExecuteException.class,
                () -> tool.callingExplorerSubAgent(List.of(new ExplorerTask(null, "explore", null, null)), null, null)
        );
        assertTrue(exception.getMessageForModel().contains("connectionId"));
    }

    @Test
    void multipleTasks_invokesConcurrently() {
        when(mockExplorer.invoke(any(SubAgentRequest.class)))
                .thenReturn(buildTestSchema("users"))
                .thenReturn(buildTestSchema("orders"));

        List<ExplorerTask> tasks = List.of(
                new ExplorerTask(1L, "explore users", null, null),
                new ExplorerTask(2L, "explore orders", null, null)
        );
        AgentToolResult result = tool.callingExplorerSubAgent(tasks, null, null);

        assertTrue(result.isSuccess());
        verify(mockExplorer, times(2)).invoke(any(SubAgentRequest.class));
    }

    @Test
    void multipleTasks_returnsTaskResultsEnvelope() {
        when(mockExplorer.invoke(any(SubAgentRequest.class)))
                .thenReturn(buildTestSchema("users"))
                .thenReturn(buildTestSchema("orders"));

        List<ExplorerTask> tasks = List.of(
                new ExplorerTask(1L, "explore users", null, null),
                new ExplorerTask(2L, "explore orders", null, null)
        );
        AgentToolResult result = tool.callingExplorerSubAgent(tasks, null, null);

        assertTrue(result.isSuccess());
        ExplorerResultEnvelope envelope = JsonUtil.json2Object((String) result.getResult(), ExplorerResultEnvelope.class);
        assertEquals(2, envelope.getTaskResults().size());
        List<String> objectNames = envelope.getTaskResults().stream()
                .map(taskResult -> taskResult.getObjects().get(0).getObjectName())
                .sorted()
                .toList();
        assertEquals(List.of("orders", "users"), objectNames);
    }

    @Test
    void multipleTasks_partialFailure_returnsSuccessEnvelope() {
        when(mockExplorer.invoke(any(SubAgentRequest.class))).thenAnswer(invocation -> {
            SubAgentRequest request = invocation.getArgument(0);
            Long connectionId = request.connectionIds().get(0);
            if (Long.valueOf(2L).equals(connectionId)) {
                throw new RuntimeException("connection timeout");
            }
            return buildTestSchema("users");
        });

        List<ExplorerTask> tasks = List.of(
                new ExplorerTask(1L, "explore users", null, null),
                new ExplorerTask(2L, "explore orders", null, null)
        );
        AgentToolResult result = tool.callingExplorerSubAgent(tasks, null, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Explorer returned partial results."));
        assertTrue(result.getMessage().contains("connectionId=2, instruction=\"explore orders\", error=connection timeout"));
        assertTrue(result.getMessage().contains("Continue only with those successful results"));
        assertTrue(result.getMessage().contains("Ask the user whether to switch connections, narrow the scope, or retry later"));
        assertTrue(result.getMessage().contains("Do not continue object discovery until the user replies"));
        ExplorerResultEnvelope envelope = JsonUtil.json2Object((String) result.getResult(), ExplorerResultEnvelope.class);
        assertEquals(2, envelope.getTaskResults().size());
        assertEquals(1, envelope.getTaskResults().stream().filter(task -> task.getStatus() == ExplorerTaskStatus.SUCCESS).count());
        assertEquals(1, envelope.getTaskResults().stream().filter(task -> task.getStatus() == ExplorerTaskStatus.ERROR).count());
        assertTrue(envelope.getTaskResults().stream().anyMatch(task -> "connection timeout".equals(task.getErrorMessage())));
    }

    @Test
    void singleTask_failure_returnsBlockingMessage() {
        when(mockExplorer.invoke(any(SubAgentRequest.class))).thenThrow(new RuntimeException("connection timeout"));

        List<ExplorerTask> tasks = List.of(new ExplorerTask(2L, "explore orders", null, null));
        AgentToolResult result = tool.callingExplorerSubAgent(tasks, null, null);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Explorer returned partial results."));
        assertTrue(result.getMessage().contains("connectionId=2, instruction=\"explore orders\", error=connection timeout"));
        assertTrue(result.getMessage().contains("Do not continue object discovery until the user replies"));
        ExplorerResultEnvelope envelope = JsonUtil.json2Object((String) result.getResult(), ExplorerResultEnvelope.class);
        assertEquals(1, envelope.getTaskResults().size());
        assertEquals(ExplorerTaskStatus.ERROR, envelope.getTaskResults().get(0).getStatus());
    }

    @Test
    void context_passedToRequest() {
        when(mockExplorer.invoke(any(SubAgentRequest.class))).thenAnswer(invocation -> {
            SubAgentRequest req = invocation.getArgument(0);
            assertEquals("previous error", req.context());
            return buildTestSchema("users");
        });

        List<ExplorerTask> tasks = List.of(new ExplorerTask(1L, "retry", "previous error", null));
        tool.callingExplorerSubAgent(tasks, null, null);

        verify(mockExplorer).invoke(any(SubAgentRequest.class));
    }

    @Test
    void taskTimeoutOverridesDefaultTimeout() {
        when(mockExplorer.invoke(any(SubAgentRequest.class))).thenAnswer(invocation -> {
            SubAgentRequest request = invocation.getArgument(0);
            assertEquals(180L, request.timeoutSeconds());
            return buildTestSchema("users");
        });

        List<ExplorerTask> tasks = List.of(new ExplorerTask(1L, "retry", null, 45L));
        AgentToolResult result = tool.callingExplorerSubAgent(tasks, 180L, null);

        assertTrue(result.isSuccess());
        verify(mockExplorer).invoke(any(SubAgentRequest.class));
    }

    @Test
    void topLevelTimeoutBelowMinimum_isRaisedToMinimum() {
        when(mockExplorer.invoke(any(SubAgentRequest.class))).thenAnswer(invocation -> {
            SubAgentRequest request = invocation.getArgument(0);
            assertEquals(180L, request.timeoutSeconds());
            return buildTestSchema("users");
        });

        List<ExplorerTask> tasks = List.of(new ExplorerTask(1L, "retry", null, null));
        AgentToolResult result = tool.callingExplorerSubAgent(tasks, 60L, null);

        assertTrue(result.isSuccess());
        verify(mockExplorer).invoke(any(SubAgentRequest.class));
    }

    @Test
    void explorerUsesDefaultTimeoutWhenNotProvided() {
        when(mockExplorer.invoke(any(SubAgentRequest.class))).thenAnswer(invocation -> {
            SubAgentRequest request = invocation.getArgument(0);
            assertEquals(180L, request.timeoutSeconds());
            return buildTestSchema("users");
        });

        List<ExplorerTask> tasks = List.of(new ExplorerTask(1L, "retry", null, null));
        AgentToolResult result = tool.callingExplorerSubAgent(tasks, null, null);

        assertTrue(result.isSuccess());
        verify(mockExplorer).invoke(any(SubAgentRequest.class));
    }
}
