package edu.zsc.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import edu.zsc.ai.model.dto.request.ChatRequest;
import edu.zsc.ai.service.ChatService;
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
 * Controller for chat operations.
 *
 * @author zgq
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Send a message to the AI.
     * Returns a stream of events.
     *
     * @param request chat request
     * @return chat response stream
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckLogin
    public Flux<Object> sendMessage(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: conversationId={}, message={}", request.getConversationId(),
                request.getMessage());
        return chatService.sendMessage(request);
    }
}
