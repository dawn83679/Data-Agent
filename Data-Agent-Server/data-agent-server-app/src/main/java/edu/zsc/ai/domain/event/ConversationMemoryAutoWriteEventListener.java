package edu.zsc.ai.domain.event;

import edu.zsc.ai.config.ExecutorConfig;
import edu.zsc.ai.domain.service.ai.autowrite.ConversationMemoryAutoWriteCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationMemoryAutoWriteEventListener {

    private final ConversationMemoryAutoWriteCoordinator autoWriteCoordinator;

    @Async(ExecutorConfig.MEMORY_AUTOWRITE_EXECUTOR_BEAN_NAME)
    @EventListener
    public void onMemoryAutoWriteRequested(ConversationMemoryAutoWriteRequestedEvent event) {
        Long conversationId = event.getConversationId();
        log.info("[MemAutoWrite] === Event received === conversationId={}", conversationId);
        try {
            autoWriteCoordinator.submit(conversationId);
            log.info("[MemAutoWrite] === Event completed === conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("[MemAutoWrite] === Event failed === conversationId={}", conversationId, e);
        }
    }
}
