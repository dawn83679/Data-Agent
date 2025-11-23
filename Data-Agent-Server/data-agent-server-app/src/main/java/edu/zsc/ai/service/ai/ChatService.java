package edu.zsc.ai.service.ai;

import edu.zsc.ai.model.dto.request.ai.ChatRequest;
import reactor.core.publisher.Flux;

public interface ChatService {

    Flux<Object> sendMessage(ChatRequest chatRequest);

}
