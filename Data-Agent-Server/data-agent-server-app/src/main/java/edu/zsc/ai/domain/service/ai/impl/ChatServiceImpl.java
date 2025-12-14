package edu.zsc.ai.domain.service.ai.impl;

import edu.zsc.ai.domain.model.dto.request.ai.ChatRequest;
import edu.zsc.ai.domain.service.ai.ChatService;
import edu.zsc.ai.domain.service.ai.manager.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private TaskManager taskManager;

    @Override
    public Flux<Object> sendMessage(ChatRequest chatRequest) {
        return taskManager.executeChatTask(chatRequest);
    }
}
