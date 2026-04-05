package edu.zsc.ai.domain.service.ai.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteContext.MemorySummary;
import edu.zsc.ai.domain.service.ai.model.MemoryWriteItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationMemoryAiWriterImplTest {

    private ChatModel qwen35PlusModel;
    private ConversationMemoryAiWriterImpl writer;

    @BeforeEach
    void setUp() {
        qwen35PlusModel = mock(ChatModel.class);
        writer = new ConversationMemoryAiWriterImpl(Map.of(
                ModelEnum.QWEN3_5_PLUS.getModelName(), qwen35PlusModel
        ));
    }

    @Test
    void extractMemoryWrites_returnsEmptyWithoutMessages() {
        List<MemoryWriteItem> items = writer.extractMemoryWrites(new MemoryWriteContext(List.of(), List.of(), false, 0));

        assertTrue(items.isEmpty());
        verify(qwen35PlusModel, never()).chat(any(ChatRequest.class));
    }

    @Test
    void extractMemoryWrites_buildsPromptAndLimitsOutputTokens() {
        stubQwenResponse("{\"items\":[]}");
        MemorySummary summary = new MemorySummary(
                12L,
                "USER",
                "PREFERENCE",
                "LANGUAGE_PREFERENCE",
                "默认回复语言",
                "默认使用中文回答",
                LocalDateTime.of(2026, 3, 15, 10, 0)
        );
        MemoryWriteContext context = new MemoryWriteContext(
                List.of(
                        message(101L, "USER", "以后默认用中文回答"),
                        message(102L, "AI", "收到，后续默认使用中文。")
                ),
                List.of(summary),
                false,
                1
        );

        writer.extractMemoryWrites(context);

        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(qwen35PlusModel).chat(requestCaptor.capture());

        ChatRequest request = requestCaptor.getValue();
        String prompt = request.messages().get(0).toString();

        assertEquals(1024, request.maxOutputTokens());
        assertTrue(prompt.contains(summary.toPromptLine()));
        assertTrue(prompt.contains("[USER]: 以后默认用中文回答"));
        assertTrue(prompt.contains("[AI]: 收到，后续默认使用中文。"));
    }

    @Test
    void extractMemoryWrites_filtersInvalidUnknownAndDuplicateItems() {
        stubQwenResponse("""
                {
                  "items": [
                    {
                      "operation": "CREATE",
                      "memoryId": null,
                      "scope": "USER",
                      "memoryType": "PREFERENCE",
                      "subType": "LANGUAGE_PREFERENCE",
                      "title": "语言偏好",
                      "content": "默认使用中文回答",
                      "reason": "用户明确要求后续中文"
                    },
                    {
                      "operation": "CREATE",
                      "memoryId": null,
                      "scope": "USER",
                      "memoryType": "PREFERENCE",
                      "subType": "LANGUAGE_PREFERENCE",
                      "title": "重复语言偏好",
                      "content": "默认使用简体中文回答",
                      "reason": "重复项应被去重"
                    },
                    {
                      "operation": "UPDATE",
                      "memoryId": 12,
                      "title": "更新后的语言偏好",
                      "content": "默认使用中文回答",
                      "reason": "用户修正了偏好"
                    },
                    {
                      "operation": "UPDATE",
                      "memoryId": 999,
                      "title": "未知记忆",
                      "content": "不应被保留",
                      "reason": "引用了不存在的记忆"
                    },
                    {
                      "operation": "DELETE",
                      "memoryId": 30
                    },
                    {
                      "operation": "DELETE",
                      "memoryId": 998
                    },
                    {
                      "operation": "CREATE",
                      "memoryId": null,
                      "scope": "USER",
                      "memoryType": "PREFERENCE",
                      "subType": "RESPONSE_FORMAT",
                      "title": "缺少内容",
                      "content": "   ",
                      "reason": "空内容"
                    },
                    {
                      "operation": "UPSERT",
                      "memoryId": 12
                    }
                  ]
                }
                """);

        List<MemoryWriteItem> items = writer.extractMemoryWrites(new MemoryWriteContext(
                List.of(message(101L, "USER", "以后默认用中文回答")),
                List.of(
                        new MemorySummary(12L, "USER", "PREFERENCE", "LANGUAGE_PREFERENCE", "默认回复语言",
                                "默认使用英文回答", LocalDateTime.of(2026, 3, 10, 9, 0)),
                        new MemorySummary(30L, "USER", "FACT", "ROLE", "职业信息",
                                "用户是 DBA", LocalDateTime.of(2026, 3, 9, 9, 0))
                ),
                false,
                2
        ));

        assertEquals(3, items.size());
        assertIterableEquals(
                List.of("CREATE", "UPDATE", "DELETE"),
                items.stream().map(MemoryWriteItem::operation).toList()
        );
        assertEquals("语言偏好", items.get(0).title());
        assertEquals(12L, items.get(1).memoryId());
        assertEquals(30L, items.get(2).memoryId());
    }

    @Test
    void extractMemoryWrites_excludesToolMessagesFromPrompt() {
        stubQwenResponse("{\"items\":[]}");
        MemoryWriteContext context = new MemoryWriteContext(
                List.of(
                        message(1L, "USER", "查一下订单表"),
                        message(2L, "AI", "好的，我来查询"),
                        message(3L, "TOOL_EXECUTION_RESULT", "{\"rows\": [{\"id\": 1, \"name\": \"test\"}]}"),
                        message(4L, "AI", "查询结果如上")
                ),
                List.of(),
                false,
                0
        );

        writer.extractMemoryWrites(context);

        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(qwen35PlusModel).chat(requestCaptor.capture());
        String prompt = requestCaptor.getValue().messages().get(0).toString();

        assertTrue(prompt.contains("[USER]: 查一下订单表"));
        assertTrue(prompt.contains("[AI]: 好的，我来查询"));
        assertFalse(prompt.contains("TOOL_EXECUTION_RESULT"));
        assertFalse(prompt.contains("rows"));
    }

    @Test
    void extractMemoryWrites_truncatesLongMessages() {
        stubQwenResponse("{\"items\":[]}");
        String longContent = "x".repeat(600);
        MemoryWriteContext context = new MemoryWriteContext(
                List.of(message(1L, "USER", longContent)),
                List.of(),
                false,
                0
        );

        writer.extractMemoryWrites(context);

        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(qwen35PlusModel).chat(requestCaptor.capture());
        String prompt = requestCaptor.getValue().messages().get(0).toString();

        assertTrue(prompt.contains("...(truncated)"));
        assertFalse(prompt.contains("x".repeat(600)));
    }

    @Test
    void extractMemoryWrites_allowsMultipleSameSubTypeCreatesForNonPreference() {
        stubQwenResponse("""
                {
                  "items": [
                    {
                      "operation": "CREATE",
                      "memoryId": null,
                      "scope": "USER",
                      "memoryType": "BUSINESS_RULE",
                      "subType": "PRODUCT_RULE",
                      "title": "Rule A",
                      "content": "All queries must include LIMIT",
                      "reason": "user stated"
                    },
                    {
                      "operation": "CREATE",
                      "memoryId": null,
                      "scope": "USER",
                      "memoryType": "BUSINESS_RULE",
                      "subType": "PRODUCT_RULE",
                      "title": "Rule B",
                      "content": "Always use LEFT JOIN not INNER JOIN",
                      "reason": "user stated"
                    }
                  ]
                }
                """);

        List<MemoryWriteItem> items = writer.extractMemoryWrites(new MemoryWriteContext(
                List.of(message(1L, "USER", "记住这两条规则")),
                List.of(),
                false,
                0
        ));

        // Both should be kept — non-PREFERENCE types allow multiple same-subType CREATEs
        assertEquals(2, items.size());
        assertEquals("Rule A", items.get(0).title());
        assertEquals("Rule B", items.get(1).title());
    }

    @Test
    void extractMemoryWrites_dedupesSameSubTypeCreatesForPreference() {
        stubQwenResponse("""
                {
                  "items": [
                    {
                      "operation": "CREATE",
                      "memoryId": null,
                      "scope": "USER",
                      "memoryType": "PREFERENCE",
                      "subType": "LANGUAGE_PREFERENCE",
                      "title": "First",
                      "content": "Use Chinese",
                      "reason": "stated"
                    },
                    {
                      "operation": "CREATE",
                      "memoryId": null,
                      "scope": "USER",
                      "memoryType": "PREFERENCE",
                      "subType": "LANGUAGE_PREFERENCE",
                      "title": "Second",
                      "content": "Use English",
                      "reason": "contradicts first"
                    }
                  ]
                }
                """);

        List<MemoryWriteItem> items = writer.extractMemoryWrites(new MemoryWriteContext(
                List.of(message(1L, "USER", "test")),
                List.of(),
                false,
                0
        ));

        // Only first PREFERENCE with same subType is kept
        assertEquals(1, items.size());
        assertEquals("First", items.get(0).title());
    }

    @Test
    void extractMemoryWrites_returnsNullWhenResponsePayloadIsInvalidJson() {
        stubQwenResponse("{not-json}");

        List<MemoryWriteItem> items = writer.extractMemoryWrites(new MemoryWriteContext(
                List.of(message(1L, "USER", "test")),
                List.of(),
                false,
                0
        ));

        assertNull(items);
    }

    @Test
    void extractMemoryWrites_returnsNullOnAiCallFailure() {
        when(qwen35PlusModel.chat(any(ChatRequest.class))).thenThrow(new RuntimeException("timeout"));

        List<MemoryWriteItem> items = writer.extractMemoryWrites(new MemoryWriteContext(
                List.of(message(1L, "USER", "test")),
                List.of(),
                false,
                0
        ));

        assertNull(items);
    }

    private void stubQwenResponse(String text) {
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(AiMessage.from(text));
        when(qwen35PlusModel.chat(any(ChatRequest.class))).thenReturn(response);
    }

    private StoredChatMessage message(Long id, String role, String data) {
        return StoredChatMessage.builder()
                .id(id)
                .role(role)
                .data(data)
                .build();
    }
}
