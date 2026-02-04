package edu.zsc.ai.controller.ai;

import cn.dev33.satoken.annotation.SaCheckLogin;
import edu.zsc.ai.domain.model.dto.request.ai.ChatRequest;
import edu.zsc.ai.domain.service.ai.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author zgq
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckLogin
    public Flux<Object> sendMessage(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: conversationId={}, message={}", request.getConversationId(),
                request.getMessage());
        return chatService.sendMessage(request);
    }
}
