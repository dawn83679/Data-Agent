package edu.zsc.ai.controller.ai;

import edu.zsc.ai.annotation.EnableRequestContext;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.ChatService;
import edu.zsc.ai.model.request.ChatRequest;
import edu.zsc.ai.model.request.SubmitToolAnswerRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chat Controller
 * Handles streaming chat requests for natural language database interactions
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@EnableRequestContext
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponseBlock> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request received: model={}, message={}, conversationId={}, connectionId={}",
                request.getModel(), request.getMessage(), request.getConversationId(), request.getConnectionId());
        return chatService.chat(request);
    }

    @PostMapping(value = "/submit-tool-answer", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponseBlock> submitToolAnswer(@Valid @RequestBody SubmitToolAnswerRequest request) {
        log.info("Submit tool answer: conversationId={}, toolCallId={}", request.getConversationId(), request.getToolCallId());
        return chatService.submitToolAnswerAndContinue(request);
    }
}
