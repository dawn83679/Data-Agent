package edu.zsc.ai.service.ai.manager;

import edu.zsc.ai.model.dto.request.ai.ChatRequest;
import reactor.core.publisher.Flux;

public interface TaskManager {

    Flux<Object> executeChatTask(ChatRequest request);
}
