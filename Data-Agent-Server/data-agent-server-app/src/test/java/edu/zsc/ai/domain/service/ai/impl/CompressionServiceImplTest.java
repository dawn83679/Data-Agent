package edu.zsc.ai.domain.service.ai.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.domain.service.ai.model.CompressionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompressionServiceImplTest {

    private ChatModel mockChatModel;
    private CompressionServiceImpl compressionService;

    @BeforeEach
    void setUp() {
        mockChatModel = mock(ChatModel.class);
        Map<String, ChatModel> chatModelsByName = Map.of(
                ModelEnum.QWEN_PLUS.getModelName(), mockChatModel
        );
        compressionService = new CompressionServiceImpl(chatModelsByName);
    }

    private void stubModelResponse(String responseText) {
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(AiMessage.from(responseText));
        when(mockChatModel.chat(any(ChatRequest.class))).thenReturn(response);
    }

    private String capturePromptText() {
        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(mockChatModel).chat(captor.capture());
        return captor.getValue().messages().get(0).toString();
    }

    // ==================== 场景 1: 带工具调用的完整数据库探索对话 ====================

    @Test
    void compress_databaseExplorationWithToolCalls_serializesToolStructure() {
        String expectedSummary = """
                ## Execution State
                - Task: explore orders table structure
                - Stage: discovery
                - Focus: inspect the verified orders schema
                - Progress: relevant environment scope and key relationships identified

                ## Grounded Facts
                - Scope: [conn-1, mydb, public]
                - Object: orders -> id (int8, PK), customer_id (int8, FK→customers.id), total (numeric), created_at (timestamp)
                - Object: customers -> id (int8, PK), name (varchar)

                ## Pending / Blocking
                - Ambiguity: none
                - Write confirmation: not_needed
                - Blocker: none
                - Next step: answer with the verified schema summary
                """;
        stubModelResponse(expectedSummary);

        // 模拟真实的数据库探索对话：用户问 → AI 调工具 → 工具返回 → AI 总结
        List<ChatMessage> messages = List.of(
                UserMessage.from("Show me the structure of the orders table"),
                // AI 发起 getEnvironmentOverview 工具调用
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-1")
                                .name("getEnvironmentOverview")
                                .arguments("{}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-1", "getEnvironmentOverview",
                        "{\"connections\":[{\"connectionId\":\"conn-1\",\"catalog\":\"mydb\",\"schemas\":[\"public\"]}]}"),
                // AI 发起 getObjectDetail 工具调用
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-2")
                                .name("getObjectDetail")
                                .arguments("{\"connectionId\":\"conn-1\",\"catalog\":\"mydb\",\"schema\":\"public\",\"objectName\":\"orders\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-2", "getObjectDetail",
                        "{\"tableName\":\"orders\",\"columns\":[{\"name\":\"id\",\"type\":\"int8\",\"pk\":true},"
                                + "{\"name\":\"customer_id\",\"type\":\"int8\",\"fk\":\"customers.id\"},"
                                + "{\"name\":\"total\",\"type\":\"numeric\"},"
                                + "{\"name\":\"created_at\",\"type\":\"timestamp\"}],"
                                + "\"rowCount\":15230}"),
                AiMessage.from("The orders table has 4 columns: id (PK), customer_id (FK to customers), total, and created_at. It contains 15,230 rows.")
        );

        CompressionResult result = compressionService.compress(messages);

        assertEquals(expectedSummary, result.summary());

        // 验证提示词中包含完整的工具调用结构（工具名、参数、结果）
        String prompt = capturePromptText();
        assertTrue(prompt.contains("getEnvironmentOverview"), "Should contain tool name");
        assertTrue(prompt.contains("getObjectDetail"), "Should contain tool name");
        assertTrue(prompt.contains("conn-1"), "Should contain connectionId from tool result");
        assertTrue(prompt.contains("orders"), "Should contain table name from tool result");
    }

    // ==================== 场景 2: SQL 执行 + 错误重试循环 ====================

    @Test
    void compress_sqlExecutionWithRetry_containsFailureAndSuccess() {
        String expectedSummary = """
                ## Execution State
                - Task: show top 10 customers by revenue
                - Stage: execution
                - Focus: reuse the corrected revenue query
                - Progress: the failed query was corrected and a verified result is available

                ## Reusable Artifacts
                - SQL [verified]: `SELECT c.name, SUM(o.total) as revenue FROM customers c JOIN orders o ON c.id = o.customer_id GROUP BY c.name ORDER BY revenue DESC LIMIT 10` -> 10 rows, top customers by revenue

                ## Pending / Blocking
                - Ambiguity: none
                - Write confirmation: not_needed
                - Blocker: none
                - Next step: answer using the verified query result
                """;
        stubModelResponse(expectedSummary);

        List<ChatMessage> messages = List.of(
                UserMessage.from("Show me top 10 customers by revenue"),
                // 第一次尝试：错误的列名
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-1")
                                .name("executeSelectSql")
                                .arguments("{\"sql\":\"SELECT c.name, SUM(o.amount) as revenue FROM customers c JOIN orders o ON c.id = o.customer_id GROUP BY c.name ORDER BY revenue DESC LIMIT 10\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-1", "executeSelectSql",
                        "{\"error\":\"ERROR: column \\\"amount\\\" does not exist. Did you mean \\\"total\\\"?\"}"),
                AiMessage.from("The column name was wrong. Let me fix it and try again."),
                // 第二次尝试：修正后成功
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-2")
                                .name("executeSelectSql")
                                .arguments("{\"sql\":\"SELECT c.name, SUM(o.total) as revenue FROM customers c JOIN orders o ON c.id = o.customer_id GROUP BY c.name ORDER BY revenue DESC LIMIT 10\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-2", "executeSelectSql",
                        "{\"columns\":[\"name\",\"revenue\"],\"rows\":[[\"Acme Corp\",\"125000.00\"],[\"Globex\",\"98000.00\"]],\"rowCount\":10}"),
                AiMessage.from("Here are the top 10 customers by revenue. Acme Corp leads with $125,000.")
        );

        CompressionResult result = compressionService.compress(messages);
        assertEquals(expectedSummary, result.summary());

        // 验证提示词包含了失败和成功两次调用，让模型能判断哪些该保留
        String prompt = capturePromptText();
        assertTrue(prompt.contains("amount"), "Should include failed SQL with wrong column");
        assertTrue(prompt.contains("total"), "Should include corrected SQL");
        assertTrue(prompt.contains("column"), "Should include error message");
        assertTrue(prompt.contains("Acme Corp"), "Should include query results");
    }

    // ==================== 场景 3: askUserQuestion 交互 ====================

    @Test
    void compress_askUserQuestionFlow_preservesQuestionAndAnswer() {
        String expectedSummary = """
                ## Execution State
                - Task: clean up old orders from before 2023
                - Stage: execution
                - Focus: the confirmed delete has already run
                - Progress: delete target counted, user confirmed, and write executed successfully

                ## Reusable Artifacts
                - SQL [verified]: `DELETE FROM orders WHERE created_at < '2023-01-01'` -> executed, affectedRows=5230

                ## Pending / Blocking
                - Ambiguity: none
                - Write confirmation: executed
                - Blocker: none
                - Next step: report the completed write outcome
                """;
        stubModelResponse(expectedSummary);

        List<ChatMessage> messages = List.of(
                UserMessage.from("Clean up old orders from before 2023"),
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-1")
                                .name("executeSelectSql")
                                .arguments("{\"sql\":\"SELECT COUNT(*) as cnt FROM orders WHERE created_at < '2023-01-01'\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-1", "executeSelectSql",
                        "{\"columns\":[\"cnt\"],\"rows\":[[\"5230\"]],\"rowCount\":1}"),
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-2")
                                .name("executeNonSelectSql")
                                .arguments("{\"sql\":\"DELETE FROM orders WHERE created_at < '2023-01-01'\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-2", "executeNonSelectSql",
                        "{\"status\":\"REQUIRES_CONFIRMATION\",\"requiresConfirmation\":true,\"ruleMatched\":false,"
                                + "\"confirmation\":{\"conversationId\":42,\"connectionId\":5,\"databaseName\":\"sales\","
                                + "\"schemaName\":\"public\",\"sql\":\"DELETE FROM orders WHERE created_at < '2023-01-01'\","
                                + "\"sqlPreview\":\"DELETE FROM orders WHERE created_at < '2023-01-01'\","
                                + "\"availableGrantOptions\":["
                                + "{\"scopeType\":\"CONVERSATION\",\"grantPreset\":\"EXACT_SCHEMA\"},"
                                + "{\"scopeType\":\"CONVERSATION\",\"grantPreset\":\"DATABASE_ALL_SCHEMAS\"},"
                                + "{\"scopeType\":\"CONVERSATION\",\"grantPreset\":\"CONNECTION_ALL_DATABASES\"},"
                                + "{\"scopeType\":\"USER\",\"grantPreset\":\"EXACT_SCHEMA\"},"
                                + "{\"scopeType\":\"USER\",\"grantPreset\":\"DATABASE_ALL_SCHEMAS\"},"
                                + "{\"scopeType\":\"USER\",\"grantPreset\":\"CONNECTION_ALL_DATABASES\"}]},"
                                + "\"message\":\"executeNonSelectSql requires user confirmation\"}"),
                UserMessage.from("I confirm this write. Please retry the exact same SQL."),
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-3")
                                .name("executeNonSelectSql")
                                .arguments("{\"sql\":\"DELETE FROM orders WHERE created_at < '2023-01-01'\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-3", "executeNonSelectSql",
                        "{\"status\":\"EXECUTED\",\"requiresConfirmation\":false,\"ruleMatched\":false,"
                                + "\"execution\":{\"success\":true,\"results\":[{\"success\":true,\"affectedRows\":5230}]},"
                                + "\"message\":\"Write SQL executed after explicit user approval.\"}"),
                AiMessage.from("Done! Successfully deleted 5,230 old orders.")
        );

        CompressionResult result = compressionService.compress(messages);
        assertEquals(expectedSummary, result.summary());

        String prompt = capturePromptText();
        // 验证新的确认流程信息都在提示词中
        assertTrue(prompt.contains("REQUIRES_CONFIRMATION"), "Should contain executeNonSelectSql confirmation status");
        assertTrue(prompt.contains("5,230"), "Should contain the row count in question");
        assertTrue(prompt.contains("I confirm this write"), "Should contain user's confirmation message");
        assertTrue(prompt.contains("DELETE FROM orders"), "Should contain the DML SQL");
        assertTrue(prompt.contains("affectedRows"), "Should contain affected rows result");
        assertFalse(prompt.contains("askUserConfirm"), "Should not reference removed askUserConfirm tool");
    }

    // ==================== 场景 4: 带 memory_context 注入的消息 ====================

    @Test
    void compress_messagesWithInjectedXmlTags_includesTagsForModelToDiscard() {
        stubModelResponse("""
                ## Execution State
                - Task: query user stats
                - Stage: execution
                - Focus: report the verified user count
                - Progress: active user count query completed

                ## Pending / Blocking
                - Ambiguity: none
                - Write confirmation: not_needed
                - Blocker: none
                - Next step: answer with the verified count
                """);

        List<ChatMessage> messages = List.of(
                UserMessage.from("<memory_context>\n- User prefers Chinese output\n</memory_context>\n"
                        + "<user_query>\nHow many active users do we have?\n</user_query>"),
                AiMessage.from("Let me check the user count."),
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-1")
                                .name("executeSelectSql")
                                .arguments("{\"sql\":\"SELECT COUNT(*) FROM users WHERE status = 'active'\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-1", "executeSelectSql",
                        "{\"columns\":[\"count\"],\"rows\":[[\"8421\"]],\"rowCount\":1}"),
                AiMessage.from("There are 8,421 active users.")
        );

        compressionService.compress(messages);

        String prompt = capturePromptText();
        // XML 标签应出现在序列化消息中，由压缩提示词指导模型丢弃
        assertTrue(prompt.contains("memory_context"), "Serialized messages should contain the XML tags");
        assertTrue(prompt.contains("Aggressively Discard"), "Prompt template should instruct to discard XML tags");
        assertTrue(prompt.contains("<user_memory>"), "Prompt should discard current runtime support wrappers as well");
    }

    // ==================== 场景 5: 多轮 searchObjects 探索 ====================

    @Test
    void compress_multipleSearchObjectsCalls_allToolCallsSerialized() {
        stubModelResponse("""
                ## Execution State
                - Task: find all tables related to orders
                - Stage: discovery
                - Focus: compare candidate order-related tables
                - Progress: multiple relevant objects identified

                ## Reusable Artifacts
                - Explorer: high-relevance objects include user_orders, order_items, and order_status_log

                ## Pending / Blocking
                - Ambiguity: determine which order-related table best matches the user's intent
                - Write confirmation: not_needed
                - Blocker: none
                - Next step: continue schema inspection or clarify the target object
                """);

        List<ChatMessage> messages = List.of(
                UserMessage.from("Find all tables related to orders"),
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-1")
                                .name("searchObjects")
                                .arguments("{\"keyword\":\"order\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-1", "searchObjects",
                        "{\"objects\":[\"user_orders\",\"order_items\",\"order_status_log\",\"order_archive\"]}"),
                AiMessage.from("I found 4 tables related to orders. Let me get details on each."),
                // 对每个表调用 getObjectDetail
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-2")
                                .name("getObjectDetail")
                                .arguments("{\"objectName\":\"user_orders\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-2", "getObjectDetail",
                        "{\"tableName\":\"user_orders\",\"columns\":[{\"name\":\"id\"},{\"name\":\"user_id\"},{\"name\":\"total\"},{\"name\":\"status\"}]}"),
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-3")
                                .name("getObjectDetail")
                                .arguments("{\"objectName\":\"order_items\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-3", "getObjectDetail",
                        "{\"tableName\":\"order_items\",\"columns\":[{\"name\":\"id\"},{\"name\":\"order_id\"},{\"name\":\"product_id\"},{\"name\":\"quantity\"},{\"name\":\"price\"}]}"),
                AiMessage.from("Here's a summary of the order-related tables and their structures.")
        );

        compressionService.compress(messages);

        String prompt = capturePromptText();
        // 验证多次工具调用都被序列化
        assertTrue(prompt.contains("searchObjects"), "Should contain searchObjects call");
        assertTrue(prompt.contains("user_orders"), "Should contain first table detail");
        assertTrue(prompt.contains("order_items"), "Should contain second table detail");
        assertTrue(prompt.contains("call-1"), "Should contain tool execution IDs");
        assertTrue(prompt.contains("call-3"), "Should contain all tool execution IDs");
    }

    // ==================== 场景 6: renderChart 可视化 ====================

    @Test
    void compress_renderChartCall_containsChartMetadata() {
        stubModelResponse("""
                ## Execution State
                - Task: show a chart of monthly revenue for 2024
                - Stage: answering
                - Focus: present the verified revenue trend visually
                - Progress: query completed and chart meaning is ready to report

                ## Reusable Artifacts
                - Chart: bar chart of monthly revenue showing growth from Jan ($50k) to Jun ($120k)

                ## Pending / Blocking
                - Ambiguity: none
                - Write confirmation: not_needed
                - Blocker: none
                - Next step: answer with the verified chart interpretation
                """);

        List<ChatMessage> messages = List.of(
                UserMessage.from("Show me a chart of monthly revenue for 2024"),
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-1")
                                .name("executeSelectSql")
                                .arguments("{\"sql\":\"SELECT DATE_TRUNC('month', created_at) as month, SUM(total) as revenue FROM orders WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01' GROUP BY month ORDER BY month\"}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-1", "executeSelectSql",
                        "{\"columns\":[\"month\",\"revenue\"],\"rows\":[[\"2024-01\",\"50000\"],[\"2024-02\",\"62000\"],[\"2024-03\",\"71000\"],[\"2024-04\",\"85000\"],[\"2024-05\",\"103000\"],[\"2024-06\",\"120000\"]],\"rowCount\":6}"),
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-2")
                                .name("renderChart")
                                .arguments("{\"chartType\":\"bar\",\"title\":\"Monthly Revenue 2024\",\"xAxis\":\"month\",\"yAxis\":\"revenue\",\"data\":[{\"month\":\"Jan\",\"revenue\":50000},{\"month\":\"Feb\",\"revenue\":62000}]}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-2", "renderChart",
                        "{\"success\":true,\"chartUrl\":\"/charts/abc123.png\"}"),
                AiMessage.from("Here's the monthly revenue chart for 2024. Revenue shows steady growth from $50k in January to $120k in June.")
        );

        compressionService.compress(messages);

        String prompt = capturePromptText();
        assertTrue(prompt.contains("renderChart"), "Should contain renderChart tool name");
        assertTrue(prompt.contains("bar"), "Should contain chart type");
        assertTrue(prompt.contains("Monthly Revenue"), "Should contain chart title");
    }

    // ==================== 场景 7: 50 条消息（接近上限）的大型对话 ====================

    @Test
    void compress_nearWindowLimit_handlesMaxMessages() {
        stubModelResponse("""
                ## Execution State
                - Task: continue the long multi-query investigation
                - Stage: execution
                - Focus: keep the latest verified query trail available
                - Progress: a long conversation handoff was compressed successfully

                ## Pending / Blocking
                - Ambiguity: none
                - Write confirmation: not_needed
                - Blocker: none
                - Next step: continue from the latest verified work state
                """);

        List<ChatMessage> messages = new ArrayList<>();
        // 模拟真实的长对话：用户提问 → AI 调工具 → 工具返回 → AI 回复，循环多次
        for (int i = 0; i < 12; i++) {
            messages.add(UserMessage.from("Query " + i + ": show data from table_" + i));
            messages.add(AiMessage.from(List.of(
                    ToolExecutionRequest.builder()
                            .id("call-" + i)
                            .name("executeSelectSql")
                            .arguments("{\"sql\":\"SELECT * FROM table_" + i + " LIMIT 5\"}")
                            .build()
            )));
            messages.add(ToolExecutionResultMessage.from("call-" + i, "executeSelectSql",
                    "{\"columns\":[\"id\",\"name\"],\"rows\":[[\"1\",\"row1\"],[\"2\",\"row2\"]],\"rowCount\":2}"));
            messages.add(AiMessage.from("Here are results from table_" + i));
        }
        // 12 轮 × 4 条 = 48 条消息，接近 50 条上限

        CompressionResult result = compressionService.compress(messages);

        assertEquals("""
                ## Execution State
                - Task: continue the long multi-query investigation
                - Stage: execution
                - Focus: keep the latest verified query trail available
                - Progress: a long conversation handoff was compressed successfully

                ## Pending / Blocking
                - Ambiguity: none
                - Write confirmation: not_needed
                - Blocker: none
                - Next step: continue from the latest verified work state
                """, result.summary());

        String prompt = capturePromptText();
        // 验证第一轮和最后一轮的内容都在
        assertTrue(prompt.contains("table_0"), "Should contain first table");
        assertTrue(prompt.contains("table_11"), "Should contain last table");
        assertTrue(prompt.contains("call-0"), "Should contain first tool call ID");
        assertTrue(prompt.contains("call-11"), "Should contain last tool call ID");
    }

    // ==================== 提示词模板验证 ====================

    @Test
    void compress_promptContainsAllCompressionRuleSections() {
        stubModelResponse("summary");

        List<ChatMessage> messages = List.of(
                UserMessage.from("test"),
                AiMessage.from("response")
        );

        compressionService.compress(messages);

        String prompt = capturePromptText();
        // 验证压缩提示词围绕“执行状态交接”组织
        assertTrue(prompt.contains("execution-state handoff"), "Missing execution-state handoff objective");
        assertTrue(prompt.contains("Preserve Execution State"), "Missing section: Preserve Execution State");
        assertTrue(prompt.contains("Tool-Specific Compression"), "Missing section: Tool-Specific Compression");
        assertTrue(prompt.contains("Conflict And Ambiguity Handling"), "Missing section: Conflict And Ambiguity Handling");
        assertTrue(prompt.contains("Aggressively Discard"), "Missing section: Aggressively Discard");

        // 验证新的输出格式章节都在模板中
        assertTrue(prompt.contains("Execution State"), "Missing output section: Execution State");
        assertTrue(prompt.contains("Grounded Facts"), "Missing output section: Grounded Facts");
        assertTrue(prompt.contains("Reusable Artifacts"), "Missing output section: Reusable Artifacts");
        assertTrue(prompt.contains("Pending / Blocking"), "Missing output section: Pending / Blocking");
        assertTrue(prompt.contains("Stage: [discovery|planning|execution|confirmation|answering]"),
                "Missing stage contract");
        assertTrue(prompt.contains("Write confirmation: [not_needed|required|confirmed|executed]"),
                "Missing write confirmation contract");

        // 验证工具和子代理压缩规则提到了关键工具名
        assertTrue(prompt.contains("getObjectDetail"), "Missing tool rule: getObjectDetail");
        assertTrue(prompt.contains("executeSelectSql"), "Missing tool rule: executeSelectSql");
        assertTrue(prompt.contains("executeNonSelectSql"), "Missing tool rule: executeNonSelectSql");
        assertTrue(prompt.contains("searchObjects"), "Missing tool rule: searchObjects");
        assertTrue(prompt.contains("getEnvironmentOverview"), "Missing tool rule: getEnvironmentOverview");
        assertTrue(prompt.contains("renderChart"), "Missing tool rule: renderChart");
        assertTrue(prompt.contains("askUserQuestion"), "Missing tool rule: askUserQuestion");
        assertTrue(prompt.contains("callingExplorerSubAgent"), "Missing tool rule: callingExplorerSubAgent");
        assertTrue(prompt.contains("callingPlannerSubAgent"), "Missing tool rule: callingPlannerSubAgent");
        assertTrue(prompt.contains("readMemory"), "Missing tool rule: readMemory");
        assertTrue(prompt.contains("writeMemory"), "Missing tool rule: writeMemory");
        assertTrue(prompt.contains("<system_context>"), "Missing current runtime wrapper discard rule");
        assertTrue(prompt.contains("<memory_context>"), "Missing legacy wrapper discard rule");
    }
}
