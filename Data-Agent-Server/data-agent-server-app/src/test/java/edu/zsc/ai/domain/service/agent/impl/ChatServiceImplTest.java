package edu.zsc.ai.domain.service.agent.impl;

import edu.zsc.ai.agent.ReActAgent;
import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.ChatSession;
import edu.zsc.ai.domain.service.agent.ChatSessionFactory;
import edu.zsc.ai.domain.service.agent.ChatStreamBridge;
import dev.langchain4j.invocation.InvocationParameters;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    @Test
    void chat_bridgesSingleSessionAndDoesNotChainContinuation() {
        ChatSessionFactory chatSessionFactory = mock(ChatSessionFactory.class);
        ChatStreamBridge chatStreamBridge = mock(ChatStreamBridge.class);
        ChatServiceImpl service = new ChatServiceImpl(chatSessionFactory, chatStreamBridge);

        ChatRequest request = new ChatRequest();
        request.setMessage("plan this");

        ChatSession session = new ChatSession(
                "qwen3-max",
                AgentModeEnum.AGENT,
                mock(ReActAgent.class),
                "memory-id",
                "plan this",
                InvocationParameters.from(Map.of()),
                99L,
                null,
                null
        );

        when(chatSessionFactory.create(request)).thenReturn(session);
        when(chatStreamBridge.bridge(same(session), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Flux.just(ChatResponseBlock.text("partial"), ChatResponseBlock.doneBlock(Map.of())));

        var blocks = service.chat(request).collectList().block();

        verify(chatSessionFactory, times(1)).create(request);
        verify(chatStreamBridge, times(1)).bridge(eq(session), org.mockito.ArgumentMatchers.any());
        assertEquals(2, blocks.size());
        assertEquals(99L, blocks.get(0).getConversationId());
        assertEquals(99L, blocks.get(1).getConversationId());
    }
}
