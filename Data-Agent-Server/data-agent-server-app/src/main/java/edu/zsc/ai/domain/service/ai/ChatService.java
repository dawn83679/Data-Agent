package edu.zsc.ai.domain.service.ai;

import edu.zsc.ai.domain.model.dto.request.ai.ChatRequest;
import reactor.core.publisher.Flux;

public interface ChatService {

    Flux<Object> sendMessage(ChatRequest chatRequest);

}
