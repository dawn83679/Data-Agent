package edu.zsc.ai.domain.service.ai.impl;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import edu.zsc.ai.config.ai.AiModelCatalog;
import edu.zsc.ai.config.ai.AiModelProperties;
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
    private AiModelCatalog aiModelCatalog;

    @BeforeEach
    void setUp() {
        mockChatModel = mock(ChatModel.class);
        AiModelProperties modelProperties = new AiModelProperties();
        aiModelCatalog = new AiModelCatalog(modelProperties);
        aiModelCatalog.initialize();
        Map<String, ChatModel> chatModelsByName = Map.of(
                aiModelCatalog.compressionModelName(), mockChatModel
        );
        compressionService = new CompressionServiceImpl(aiModelCatalog, chatModelsByName);
    }

    @Test
    void defaultCompressionModel_isQwen36Plus() {
        assertEquals("qwen3.6-plus", aiModelCatalog.compressionModelName());
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
                ## 执行状态
                - 任务： explore orders table structure
                - 阶段：发现
                - 焦点： inspect the verified orders schema
                - 进展： relevant environment scope and key relationships identified

                ## 已验证事实
                - 范围： [conn-1, mydb, public]
                - 对象： orders -> id (int8, PK), customer_id (int8, FK→customers.id), total (numeric), created_at (timestamp)
                - 对象： customers -> id (int8, PK), name (varchar)

                ## 待处理 / 阻塞
                - 歧义：无
                - 写操作确认：不需要
                - 阻塞：无
                - 下一步： answer with the verified schema summary
                """;
        stubModelResponse(expectedSummary);

        // 模拟真实的数据库探索对话：用户问 → AI 调工具 → 工具返回 → AI 总结
        List<ChatMessage> messages = List.of(
                UserMessage.from("Show me the structure of the orders table"),
                // AI 发起 getDatabases 工具调用
                AiMessage.from(List.of(
                        ToolExecutionRequest.builder()
                                .id("call-1")
                                .name("getDatabases")
                                .arguments("{\"connectionId\":1}")
                                .build()
                )),
                ToolExecutionResultMessage.from("call-1", "getDatabases",
                        "[\"mydb\"]"),
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
        assertTrue(prompt.contains("getDatabases"), "Should contain tool name");
        assertTrue(prompt.contains("getObjectDetail"), "Should contain tool name");
        assertTrue(prompt.contains("mydb"), "Should contain database name from tool result");
        assertTrue(prompt.contains("orders"), "Should contain table name from tool result");
    }

    // ==================== 场景 2: SQL 执行 + 错误重试循环 ====================

    @Test
    void compress_sqlExecutionWithRetry_containsFailureAndSuccess() {
        String expectedSummary = """
                ## 执行状态
                - 任务： show top 10 customers by revenue
                - 阶段：执行
                - 焦点： reuse the corrected revenue query
                - 进展： the failed query was corrected and a verified result is available

                ## 可复用产物
                - SQL [verified]: `SELECT c.name, SUM(o.total) as revenue FROM customers c JOIN orders o ON c.id = o.customer_id GROUP BY c.name ORDER BY revenue DESC LIMIT 10` -> 10 rows, top customers by revenue

                ## 待处理 / 阻塞
                - 歧义：无
                - 写操作确认：不需要
                - 阻塞：无
                - 下一步： answer using the verified query result
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
                ## 执行状态
                - 任务： clean up old orders from before 2023
                - 阶段：执行
                - 焦点： the confirmed delete has already run
                - 进展： delete target counted, user confirmed, and write executed successfully

                ## 可复用产物
                - SQL [verified]: `DELETE FROM orders WHERE created_at < '2023-01-01'` -> executed, affectedRows=5230

                ## 待处理 / 阻塞
                - 歧义：无
                - 写操作确认：已执行
                - 阻塞：无
                - 下一步： report the completed write outcome
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
                ## 执行状态
                - 任务： query user stats
                - 阶段：执行
                - 焦点： report the verified user count
                - 进展： active user count query completed

                ## 待处理 / 阻塞
                - 歧义：无
                - 写操作确认：不需要
                - 阻塞：无
                - 下一步： answer with the verified count
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
        assertTrue(prompt.contains("强制丢弃"), "Prompt template should instruct to discard XML tags");
        assertTrue(prompt.contains("<范围提示>"), "Prompt should discard current runtime support wrappers as well");
        assertTrue(prompt.contains("<任务>"), "Prompt should discard current task wrapper as well");
    }

    // ==================== 场景 5: 多轮 searchObjects 探索 ====================

    @Test
    void compress_multipleSearchObjectsCalls_allToolCallsSerialized() {
        stubModelResponse("""
                ## 执行状态
                - 任务： find all tables related to orders
                - 阶段：发现
                - 焦点： compare candidate order-related tables
                - 进展： multiple relevant objects identified

                ## 可复用产物
                - 探索： high-relevance objects include user_orders, order_items, and order_status_log

                ## 待处理 / 阻塞
                - 歧义：判断哪个订单相关表最符合用户意图
                - 写操作确认：不需要
                - 阻塞：无
                - 下一步： continue schema inspection or clarify the target object
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
                ## 执行状态
                - 任务： show a chart of monthly revenue for 2024
                - 阶段：回答
                - 焦点： present the verified revenue trend visually
                - 进展： query completed and chart meaning is ready to report

                ## 可复用产物
                - 图表： bar chart of monthly revenue showing growth from Jan ($50k) to Jun ($120k)

                ## 待处理 / 阻塞
                - 歧义：无
                - 写操作确认：不需要
                - 阻塞：无
                - 下一步： answer with the verified chart interpretation
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
                ## 执行状态
                - 任务： continue the long multi-query investigation
                - 阶段：执行
                - 焦点： keep the latest verified query trail available
                - 进展： a long conversation handoff was compressed successfully

                ## 待处理 / 阻塞
                - 歧义：无
                - 写操作确认：不需要
                - 阻塞：无
                - 下一步： continue from the latest verified work state
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
                ## 执行状态
                - 任务： continue the long multi-query investigation
                - 阶段：执行
                - 焦点： keep the latest verified query trail available
                - 进展： a long conversation handoff was compressed successfully

                ## 待处理 / 阻塞
                - 歧义：无
                - 写操作确认：不需要
                - 阻塞：无
                - 下一步： continue from the latest verified work state
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
        assertTrue(prompt.contains("执行状态交接"), "Missing 执行状态交接 objective");
        assertTrue(prompt.contains("保留执行状态"), "Missing section: 保留执行状态");
        assertTrue(prompt.contains("按工具压缩"), "Missing section: 按工具压缩");
        assertTrue(prompt.contains("冲突和歧义处理"), "Missing section: 冲突和歧义处理");
        assertTrue(prompt.contains("强制丢弃"), "Missing section: 强制丢弃");

        // 验证新的输出格式章节都在模板中
        assertTrue(prompt.contains("执行状态"), "Missing output section: 执行状态");
        assertTrue(prompt.contains("已验证事实"), "Missing output section: 已验证事实");
        assertTrue(prompt.contains("可复用产物"), "Missing output section: 可复用产物");
        assertTrue(prompt.contains("待处理 / 阻塞"), "Missing output section: 待处理 / 阻塞");
        assertTrue(prompt.contains("阶段：[发现|规划|执行|确认|回答]"),
                "Missing stage contract");
        assertTrue(prompt.contains("写操作确认：[不需要|需要|已确认|已执行]"),
                "Missing write confirmation contract");

        // 验证工具和子代理压缩规则提到了关键工具名
        assertTrue(prompt.contains("getObjectDetail"), "Missing tool rule: getObjectDetail");
        assertTrue(prompt.contains("executeSelectSql"), "Missing tool rule: executeSelectSql");
        assertTrue(prompt.contains("executeNonSelectSql"), "Missing tool rule: executeNonSelectSql");
        assertTrue(prompt.contains("searchObjects"), "Missing tool rule: searchObjects");
        assertTrue(prompt.contains("getDatabases"), "Missing tool rule: getDatabases");
        assertTrue(prompt.contains("renderChart"), "Missing tool rule: renderChart");
        assertTrue(prompt.contains("askUserQuestion"), "Missing tool rule: askUserQuestion");
        assertTrue(prompt.contains("callingExplorerSubAgent"), "Missing tool rule: callingExplorerSubAgent");
        assertTrue(prompt.contains("callingPlannerSubAgent"), "Missing tool rule: callingPlannerSubAgent");
        assertFalse(prompt.contains("readMemory"), "Compression prompt should no longer mention readMemory");
        assertFalse(prompt.contains("updateMemory"), "Compression prompt should no longer mention updateMemory");
        assertTrue(prompt.contains("<系统上下文>"), "Missing current runtime wrapper discard rule");
        assertTrue(prompt.contains("<范围提示>"), "Missing current scope wrapper discard rule");
        assertTrue(prompt.contains("<任务>"), "Missing current task wrapper discard rule");
        assertTrue(prompt.contains("<memory_context>"), "Missing legacy wrapper discard rule");
    }
}
