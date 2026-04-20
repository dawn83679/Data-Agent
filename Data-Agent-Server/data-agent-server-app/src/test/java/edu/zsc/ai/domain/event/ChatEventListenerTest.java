package edu.zsc.ai.domain.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.AiMessageService;

@ExtendWith(MockitoExtension.class)
class ChatEventListenerTest {

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    @Mock
    private AiMessageService aiMessageService;

    @Mock
    private AiConversationService aiConversationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatEventListener listener;

    @Test
    void onChatCompleted_updatesTokensAndPublishesAutoWriteRequest() {
        ChatCompletedEvent event = new ChatCompletedEvent(this, 88L, 32, 120);

        listener.onChatCompleted(event);

        verify(aiMessageService).updateLastAiMessageTokenCount(88L, 32);
        verify(aiConversationService).updateTokenCount(88L, 120);

        ArgumentCaptor<ConversationMemoryAutoWriteRequestedEvent> captor =
                ArgumentCaptor.forClass(ConversationMemoryAutoWriteRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(88L, captor.getValue().getConversationId());
    }

    @Test
    void onChatCompleted_withoutTokenUsage_stillPublishesAutoWriteRequest() {
        ChatCompletedEvent event = new ChatCompletedEvent(this, 88L, null, null);

        listener.onChatCompleted(event);

        verify(aiMessageService, never()).updateLastAiMessageTokenCount(anyLong(), anyInt());
        verify(aiConversationService).updateTokenCount(88L, null);

        ArgumentCaptor<ConversationMemoryAutoWriteRequestedEvent> captor =
                ArgumentCaptor.forClass(ConversationMemoryAutoWriteRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(88L, captor.getValue().getConversationId());
    }
}
