package edu.zsc.ai.agent.tool.orchestrator;

import edu.zsc.ai.agent.subagent.contract.*;
import edu.zsc.ai.agent.subagent.explorer.ExplorerSubAgent;
import edu.zsc.ai.agent.subagent.planner.PlannerSubAgent;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.config.ai.SubAgentProperties;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.util.JsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CallingPlannerToolTest {

    private PlannerSubAgent mockPlanner;
    private CallingPlannerTool tool;

    @BeforeEach
    void setUp() {
        ExplorerSubAgent mockExplorer = mock(ExplorerSubAgent.class);
        mockPlanner = mock(PlannerSubAgent.class);
        SubAgentProperties properties = new SubAgentProperties();
        SubAgentManager subAgentManager = new SubAgentManager(mockExplorer, mockPlanner, properties);
        tool = new CallingPlannerTool(subAgentManager, new DefaultSchemaSummaryResolver());
    }

    @AfterEach
    void tearDown() {
        AgentExecutionContext.clear();
        RequestContext.clear();
    }

    private SchemaSummary buildTestSchema() {
        return SchemaSummary.builder()
                .objects(List.of(
                        ExploreObject.builder()
                                .catalog("analytics")
                                .schema("public")
                                .objectName("users")
                                .objectType("TABLE")
                                .objectDdl("CREATE TABLE users (id int8)")
                                .relevanceScore(90)
                                .build()
                ))
                .rawResponse("users object found")
                .build();
    }

    @Test
    void invokesPlanner() {
        SqlPlan plan = SqlPlan.builder()
                .summaryText("Generate a trivial query.")
                .sqlBlocks(List.of(
                        SqlPlanBlock.builder()
                                .title("Final SQL")
                                .sql("SELECT 1")
                                .kind(SqlPlanBlockKind.FINAL)
                                .build()
                ))
                .rawResponse("Use SELECT 1 as the final SQL.")
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenReturn(plan);

        String schemaJson = JsonUtil.object2json(buildTestSchema());
        AgentToolResult result = tool.callingPlannerSubAgent(
                "generate revenue query", schemaJson, null, null);

        assertTrue(result.isSuccess());
        verify(mockPlanner).invoke(any(PlannerRequest.class));
    }

    @Test
    void missingSchemaSummary_fails() {
        AgentToolExecuteException exception = assertThrows(
                AgentToolExecuteException.class,
                () -> tool.callingPlannerSubAgent("generate query", null, null, null)
        );
        assertTrue(exception.getMessageForModel().contains("schemaSummaryJson"));
    }

    @Test
    void invalidSchemaJson_fails() {
        AgentToolExecuteException exception = assertThrows(
                AgentToolExecuteException.class,
                () -> tool.callingPlannerSubAgent("generate query", "invalid json{{{", null, null)
        );
        assertTrue(exception.getMessageForModel().contains("parse"));
    }

    @Test
    void validSchemaJson_returnsRawResponse() {
        SqlPlan plan = SqlPlan.builder()
                .summaryText("Aggregate total revenue from orders.")
                .sqlBlocks(List.of(
                        SqlPlanBlock.builder()
                                .title("Final SQL")
                                .sql("SELECT SUM(total) FROM orders")
                                .kind(SqlPlanBlockKind.FINAL)
                                .build()
                ))
                .rawResponse("Aggregate the orders table to compute total revenue.")
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenReturn(plan);

        String schemaJson = JsonUtil.object2json(buildTestSchema());
        AgentToolResult result = tool.callingPlannerSubAgent(
                "calculate total revenue", schemaJson, null, null);

        assertTrue(result.isSuccess());
        String resultText = (String) result.getResult();
        assertTrue(resultText.contains("summaryText"));
        assertTrue(resultText.contains("SELECT SUM(total)"));
    }

    @Test
    void instructionWithOptimizationContext_passedThrough() {
        SqlPlan plan = SqlPlan.builder()
                .summaryText("Optimize the provided SQL.")
                .sqlBlocks(List.of(
                        SqlPlanBlock.builder()
                                .title("Final SQL")
                                .sql("SELECT 1")
                                .kind(SqlPlanBlockKind.FINAL)
                                .build()
                ))
                .rawResponse("Optimized SQL produced.")
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenAnswer(invocation -> {
            PlannerRequest req = invocation.getArgument(0);
            assertTrue(req.getInstruction().contains("optimize"));
            return plan;
        });

        String schemaJson = JsonUtil.object2json(buildTestSchema());
        tool.callingPlannerSubAgent(
                "optimize: SELECT * FROM old. DDL: CREATE TABLE old (id int). Index: idx_old_id",
                schemaJson, null, null);

        verify(mockPlanner).invoke(any(PlannerRequest.class));
    }

    @Test
    void explorerEnvelope_isAcceptedAsSchemaInput() {
        SqlPlan plan = SqlPlan.builder()
                .summaryText("Query users.")
                .sqlBlocks(List.of(
                        SqlPlanBlock.builder()
                                .title("Final SQL")
                                .sql("SELECT * FROM users")
                                .kind(SqlPlanBlockKind.FINAL)
                                .build()
                ))
                .rawResponse("Use the users table directly.")
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenReturn(plan);

        ExplorerResultEnvelope envelope = ExplorerResultEnvelope.builder()
                .taskResults(List.of(
                        ExplorerTaskResult.builder()
                                .taskId("explore-1")
                                .summaryText("Relevant objects: users (TABLE).")
                                .objects(buildTestSchema().getObjects())
                                .rawResponse(buildTestSchema().getRawResponse())
                                .build()
                ))
                .build();

        AgentToolResult result = tool.callingPlannerSubAgent(
                "generate query",
                JsonUtil.object2json(envelope),
                null,
                null);

        assertTrue(result.isSuccess());
        verify(mockPlanner).invoke(any(PlannerRequest.class));
    }

    @Test
    void explorerEnvelope_skipsFailedTasksWhenBuildingSchema() {
        SqlPlan plan = SqlPlan.builder()
                .summaryText("Query users.")
                .sqlBlocks(List.of(
                        SqlPlanBlock.builder()
                                .title("Final SQL")
                                .sql("SELECT * FROM users")
                                .kind(SqlPlanBlockKind.FINAL)
                                .build()
                ))
                .rawResponse("Use the users table directly.")
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenAnswer(invocation -> {
            PlannerRequest request = invocation.getArgument(0);
            assertEquals(1, request.getSchemaSummary().getObjects().size());
            assertEquals("users", request.getSchemaSummary().getObjects().get(0).getObjectName());
            return plan;
        });

        ExplorerResultEnvelope envelope = ExplorerResultEnvelope.builder()
                .taskResults(List.of(
                        ExplorerTaskResult.builder()
                                .taskId("explore-1")
                                .status(ExplorerTaskStatus.SUCCESS)
                                .summaryText("Relevant objects: users (TABLE).")
                                .objects(buildTestSchema().getObjects())
                                .rawResponse(buildTestSchema().getRawResponse())
                                .build(),
                        ExplorerTaskResult.builder()
                                .taskId("explore-2")
                                .status(ExplorerTaskStatus.ERROR)
                                .objects(List.of())
                                .rawResponse("")
                                .errorMessage("connection timeout")
                                .build()
                ))
                .build();

        AgentToolResult result = tool.callingPlannerSubAgent(
                "generate query",
                JsonUtil.object2json(envelope),
                null,
                null);

        assertTrue(result.isSuccess());
        verify(mockPlanner).invoke(any(PlannerRequest.class));
    }

    @Test
    void plannerInvocation_setsTaskIdInExecutionContext() {
        RequestContext.set(RequestContextInfo.builder()
                .conversationId(42L)
                .userId(7L)
                .build());

        SqlPlan plan = SqlPlan.builder()
                .summaryText("Query users.")
                .sqlBlocks(List.of(
                        SqlPlanBlock.builder()
                                .title("Final SQL")
                                .sql("SELECT * FROM users")
                                .kind(SqlPlanBlockKind.FINAL)
                                .build()
                ))
                .rawResponse("Use the users table directly.")
                .build();

        when(mockPlanner.invoke(any(PlannerRequest.class))).thenAnswer(invocation -> {
            String taskId = AgentExecutionContext.getTaskId();
            assertNotNull(taskId);
            assertTrue(taskId.startsWith("plan-42-"));
            return plan;
        });

        String schemaJson = JsonUtil.object2json(buildTestSchema());
        AgentToolResult result = tool.callingPlannerSubAgent(
                "generate query",
                schemaJson,
                null,
                null);

        assertTrue(result.isSuccess());
        assertNull(AgentExecutionContext.getTaskId());
        verify(mockPlanner).invoke(any(PlannerRequest.class));
    }

    @Test
    void plannerTimeout_isPassedToRequest() {
        SqlPlan plan = SqlPlan.builder()
                .summaryText("Query users.")
                .sqlBlocks(List.of(
                        SqlPlanBlock.builder()
                                .title("Final SQL")
                                .sql("SELECT * FROM users")
                                .kind(SqlPlanBlockKind.FINAL)
                                .build()
                ))
                .rawResponse("Use the users table directly.")
                .build();
        when(mockPlanner.invoke(any(PlannerRequest.class))).thenAnswer(invocation -> {
            PlannerRequest request = invocation.getArgument(0);
            assertEquals(75L, request.getTimeoutSeconds());
            return plan;
        });

        String schemaJson = JsonUtil.object2json(buildTestSchema());
        AgentToolResult result = tool.callingPlannerSubAgent(
                "generate query",
                schemaJson,
                75L,
                null);

        assertTrue(result.isSuccess());
        verify(mockPlanner).invoke(any(PlannerRequest.class));
    }
}
