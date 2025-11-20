package edu.zsc.ai.service.impl.ai;

import edu.zsc.ai.enums.ai.ChatStatusEnum;
import edu.zsc.ai.model.dto.request.ai.ChatRequest;
import edu.zsc.ai.model.dto.response.ai.ChatBlockResponse;
import edu.zsc.ai.model.dto.response.ai.message.HistoryContextResponse;
import edu.zsc.ai.model.dto.response.ai.message.HistoryMessage;
import edu.zsc.ai.service.manager.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;


@Slf4j
@Service
public class TaskManagerImpl implements TaskManager {

    @Autowired
    private ConversationManager conversationManager;
    @Autowired
    private ContextManager contextManager;
    @Autowired
    private MessageStorage messageStorage;
    @Autowired
    private ToolCallingManager toolCallingManager;
    @Autowired
    private ConcurrencyManager concurrencyManager;

    @Autowired
    private ChatClient chatClient;

    private static final int MAX_TOKEN = 10000;

    @Override
    public Flux<Object> executeChatTask(ChatRequest request) {
        log.debug("Starting chat task processing, conversationId: {}, message: {}", request.getConversationId(), request.getMessage());

        return Flux.create(sink -> {
            try {
                //1. create or get conversation & check conversation status(if Processing, enqueue chatRequest by ConcurrencyManager)

                // Step 1: Create or get conversation
                Long conversationId = conversationManager.createOrGetConversation(
                        request.getConversationId(),
                        request.getMessage()
                );
                log.debug("Retrieved conversation ID: {}", conversationId);

                // Check concurrency status, try to lock conversation
                if (!concurrencyManager.tryLockConversation(conversationId)) {
                    log.debug("Conversation {} is being processed, adding request to queue", conversationId);

                    // Send queued status to client
                    sink.next(ChatBlockResponse.builder()
                            .conversationId(conversationId)
                            .status(ChatStatusEnum.QUEUED.name())
                            .data("Your request has been added to the queue, please wait...")
                            .build());

                    // Add request to queue
                    concurrencyManager.enqueueMessage(conversationId, request);
                    sink.complete();
                    return;
                }

                log.debug("Successfully locked conversation {}", conversationId);

                // Use try-finally to ensure lock is always released
                try {
                    //2. get history messages
                    log.debug("Getting history messages for conversation {}", conversationId);
                    HistoryContextResponse historyContext = contextManager.getContextForAI(conversationId);
                    log.debug("Retrieved {} history messages", historyContext.getMessages().size());

                    //3. save user message
                    log.debug("Saving user message to conversation {}", conversationId);
                    Long userMessageId = messageStorage.saveUserMessage(conversationId, request.getMessage());
                    log.debug("User message saved with ID: {}", userMessageId);

                    /*4. build context
                     *    4.1 calculate history messages tokens
                     *        4.1.1 if exceed max tokens, compress history messages(but not now)
                     *    4.2 add system prompt
                     *    4.3 add user message
                     *    4.4 add relevant knowledge base contents (but not now)
                     **/
                    Integer totalTokenCount = historyContext.getTotalTokenCount();
                    if (totalTokenCount > MAX_TOKEN) {
                        historyContext = contextManager.compressContext(conversationId, historyContext, MAX_TOKEN);
                    }
                    ArrayList<Message> messages = new ArrayList<>(historyContext.getMessages().size() + 2);
                    messages.addAll(historyContext.getMessages().stream().map(HistoryMessage::getMessage).toList());
                    messages.add(new SystemMessage("You are a helpful AI assistant."));
                    messages.add(new UserMessage(request.getMessage()));
                    /*5. call chat model API
                     *    5.1 realtime parse model output chunk, if tool calling needed, call toolCallingManager
                     *    5.2 realtime emit output chunk to client
                     *    5.3 realtime save assistant output chunk
                     *    5.4 we need get input/output tokens form the last chatResponse chunk, through chatResponse.getMetadata().getUsage(), and then save user input and assistant output tokens
                     *    5.5 loop chat
                     *        5.5.1 we need put the tool result(UserMessage) into context and call chat model API again
                     *        5.5.2 concurrencyManager.getAllQueuedMessages() to get queued messages, put them into context and call chat model API again
                     *        5.5.3 until no tool calling needed and no queued messages
                     *    5.4 record all message tokens count into AiConversation
                     **/
                    sink.complete();

                } finally {
                    // Ensure conversation lock is released
                    concurrencyManager.unlockConversation(conversationId);
                    log.debug("Released conversation lock: {}", conversationId);
                }

            } catch (Exception e) {
                log.error("Exception occurred while processing chat task", e);
                sink.error(e);
            }
        });
    }
}
