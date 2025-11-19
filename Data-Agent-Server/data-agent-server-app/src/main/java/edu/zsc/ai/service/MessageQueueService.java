package edu.zsc.ai.service;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;




/**
 * Service for managing message queues for concurrent user inputs.
 * Ensures that messages are processed sequentially for each conversation.
 *
 * @author zgq
 * @since 0.0.1
 */
@Service
public class MessageQueueService {

    private final ConcurrentHashMap<Long, Queue<String>> messageQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> processingStatus = new ConcurrentHashMap<>();

    /**
     * Enqueue a message for a specific conversation.
     *
     * @param conversationId the conversation ID
     * @param message        the user message
     */
    public void enqueue(Long conversationId, String message) {
        messageQueues.computeIfAbsent(conversationId, k -> new ConcurrentLinkedQueue<>()).offer(message);
    }

    /**
     * Retrieve and remove the next message from the queue.
     *
     * @param conversationId the conversation ID
     * @return the next message, or null if the queue is empty
     */
    public String processNext(Long conversationId) {
        Queue<String> queue = messageQueues.get(conversationId);
        if (queue != null) {
            String message = queue.poll();
            if (queue.isEmpty()) {
                messageQueues.remove(conversationId);
            }
            return message;
        }
        return null;
    }

    /**
     * Check if the conversation is currently being processed.
     *
     * @param conversationId the conversation ID
     * @return true if processing, false otherwise
     */
    public boolean isProcessing(Long conversationId) {
        return processingStatus.getOrDefault(conversationId, false);
    }

    /**
     * Set the processing status for a conversation.
     *
     * @param conversationId the conversation ID
     * @param processing     true if processing, false otherwise
     */
    public void setProcessing(Long conversationId, boolean processing) {
        if (processing) {
            processingStatus.put(conversationId, true);
        } else {
            processingStatus.remove(conversationId);
        }
    }

    /**
     * Check if there are pending messages in the queue.
     * 
     * @param conversationId the conversation ID
     * @return true if there are pending messages
     */
    public boolean hasPendingMessages(Long conversationId) {
        Queue<String> queue = messageQueues.get(conversationId);
        return queue != null && !queue.isEmpty();
    }

    /**
     * Retrieve and remove all messages from the queue.
     * 
     * @param conversationId the conversation ID
     * @return list of all queued messages, empty list if no messages
     */
    public List<String> getAllQueuedMessages(Long conversationId) {
        Queue<String> queue = messageQueues.remove(conversationId);
        if (CollectionUtils.isEmpty(queue)) {
            return new ArrayList<>(queue);
        }
        return Collections.emptyList();
    }
}
