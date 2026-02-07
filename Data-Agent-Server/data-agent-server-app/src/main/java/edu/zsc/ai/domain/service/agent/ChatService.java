package edu.zsc.ai.domain.service.agent;

import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.model.request.ChatRequest;
import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<ChatResponseBlock> chat(ChatRequest request);
}
