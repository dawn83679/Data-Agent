package edu.zsc.ai.service.impl.manager;

import edu.zsc.ai.model.dto.request.ai.ChatRequest;
import edu.zsc.ai.service.ai.manager.ConcurrencyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
public class ConcurrencyManagerImpl implements ConcurrencyManager {

    private final Set<Long> activeConversations = ConcurrentHashMap.newKeySet();

    private final Map<Long, Queue<ChatRequest>> messageQueues = new ConcurrentHashMap<>();

    @Override
    public boolean tryLockConversation(Long conversationId) {
        boolean locked = activeConversations.add(conversationId);
        if (locked) {
            log.debug("Conversation {} locked successfully", conversationId);
        } else {
            log.debug("Conversation {} is already locked (busy)", conversationId);
        }
        return locked;
    }

    @Override
    public void unlockConversation(Long conversationId) {
        boolean removed = activeConversations.remove(conversationId);
        if (removed) {
            log.debug("Conversation {} unlocked", conversationId);
        } else {
            log.warn("Attempted to unlock conversation {} which was not locked", conversationId);
        }
    }

    @Override
    public void enqueueMessage(Long conversationId, ChatRequest request) {
        messageQueues.computeIfAbsent(conversationId, k -> new ConcurrentLinkedQueue<>())
                .add(request);
        log.debug("Message enqueued for conversation {}. Queue size: {}",
                conversationId, messageQueues.get(conversationId).size());
    }

    @Override
    public List<ChatRequest> getAllQueuedMessages(Long conversationId) {
        Queue<ChatRequest> queue = messageQueues.get(conversationId);
        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatRequest> pendingMessages = new ArrayList<>();
        ChatRequest request;
        while ((request = queue.poll()) != null) {
            pendingMessages.add(request);
        }

        log.debug("Retrieved {} queued messages for conversation {}", pendingMessages.size(), conversationId);
        return pendingMessages;
    }
}
