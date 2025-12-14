package edu.zsc.ai.domain.service.ai.manager;

import edu.zsc.ai.domain.model.dto.request.ai.ChatRequest;
import reactor.core.publisher.Flux;

public interface TaskManager {

    Flux<Object> executeChatTask(ChatRequest request);
}
