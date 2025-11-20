package edu.zsc.ai.service.impl.ai;

import edu.zsc.ai.model.dto.request.ai.ChatRequest;
import edu.zsc.ai.service.ChatService;
import edu.zsc.ai.service.manager.TaskManager;
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
