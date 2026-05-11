package edu.zsc.ai.domain.service.ai.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import edu.zsc.ai.common.enums.ai.MessageStatusEnum;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMessageServiceImplTest {

    @Test
    void replaceConversationMessages_compressionSyncMarksOldActivePrefixAndAppendsSummary() {
        ChatMessage oldUser = UserMessage.from("old user");
        ChatMessage oldAi = AiMessage.from("old ai");
        ChatMessage keptUser = UserMessage.from("kept user");
        ChatMessage keptAi = AiMessage.from("kept ai");
        TestAiMessageService service = new TestAiMessageService(List.of(
                stored(1L, oldUser, MessageStatusEnum.NORMAL, 0),
                stored(2L, oldAi, MessageStatusEnum.NORMAL, 1),
                stored(3L, keptUser, MessageStatusEnum.NORMAL, 2),
                stored(4L, keptAi, MessageStatusEnum.NORMAL, 3)
        ));

        ChatMessage context = compactionContext("old history");
        service.replaceConversationMessages(10L, List.of(context, keptUser, keptAi));

        assertEquals(MessageStatusEnum.COMPRESSED.getCode(), service.message(1L).getStatus());
        assertEquals(MessageStatusEnum.COMPRESSED.getCode(), service.message(2L).getStatus());
        assertEquals(MessageStatusEnum.NORMAL.getCode(), service.message(3L).getStatus());
        assertEquals(MessageStatusEnum.NORMAL.getCode(), service.message(4L).getStatus());
        assertEquals(5, service.messages.size());

        StoredChatMessage appended = service.messages.get(4);
        assertEquals("SYSTEM", appended.getRole());
        assertEquals(MessageStatusEnum.COMPRESSION_SUMMARY.getCode(), appended.getStatus());
        assertInstanceOf(SystemMessage.class, ChatMessageDeserializer.messageFromJson(appended.getData()));
        assertTrue(appended.getCreatedAt().isAfter(service.message(4L).getCreatedAt()));
    }

    @Test
    void replaceConversationMessages_keepsIncomingOrderWhenNoCompressionSync() {
        ChatMessage context = compactionContext("history");
        ChatMessage user = UserMessage.from("u1");
        TestAiMessageService service = new TestAiMessageService(List.of());

        service.replaceConversationMessages(10L, List.of(context, user));

        assertEquals(2, service.messages.size());
        assertEquals("SYSTEM", service.messages.get(0).getRole());
        assertEquals(MessageStatusEnum.COMPRESSION_SUMMARY.getCode(), service.messages.get(0).getStatus());
        assertEquals("USER", service.messages.get(1).getRole());
        assertEquals(MessageStatusEnum.NORMAL.getCode(), service.messages.get(1).getStatus());
    }

    private static StoredChatMessage stored(Long id, ChatMessage message, MessageStatusEnum status, int offsetNanos) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 11, 10, 0).plusNanos(offsetNanos * 1000L);
        return StoredChatMessage.builder()
                .id(id)
                .conversationId(10L)
                .role(message.type().name())
                .tokenCount(0)
                .data(ChatMessageSerializer.messageToJson(message))
                .status(status.getCode())
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private static ChatMessage compactionContext(String body) {
        return SystemMessage.from("""
                This Data-Agent conversation continues from compacted context. Use the summary below as prior-task state for database and tool work. Do not ask follow-up questions unless they are required to complete the user's request.

                Summary:
                %s

                Recent messages are preserved verbatim after this compaction context.

                Resume the conversation directly from the latest user message. Do not mention this compaction context.""".formatted(body));
    }

    private static final class TestAiMessageService extends AiMessageServiceImpl {
        private final List<StoredChatMessage> messages = new ArrayList<>();
        private long nextId = 100L;

        private TestAiMessageService(List<StoredChatMessage> seed) {
            messages.addAll(seed);
        }

        @Override
        public List<StoredChatMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId) {
            return messages.stream()
                    .filter(message -> conversationId.equals(message.getConversationId()))
                    .sorted((left, right) -> {
                        int created = left.getCreatedAt().compareTo(right.getCreatedAt());
                        if (created != 0) {
                            return created;
                        }
                        return Long.compare(left.getId(), right.getId());
                    })
                    .toList();
        }

        @Override
        public void saveBatchMessages(List<StoredChatMessage> toSave) {
            for (StoredChatMessage message : toSave) {
                message.setId(nextId++);
                messages.add(message);
            }
        }

        @Override
        protected void markCompressedByIds(List<Long> ids) {
            for (StoredChatMessage message : messages) {
                if (ids.contains(message.getId())) {
                    message.setStatus(MessageStatusEnum.COMPRESSED.getCode());
                }
            }
        }

        @Override
        public boolean removeByIds(java.util.Collection<?> list) {
            messages.removeIf(message -> list.contains(message.getId()));
            return true;
        }

        private StoredChatMessage message(Long id) {
            return messages.stream()
                    .filter(message -> id.equals(message.getId()))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
