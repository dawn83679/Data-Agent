package edu.zsc.ai.service.impl;

import edu.zsc.ai.model.dto.request.ChatRequest;
import edu.zsc.ai.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service("coreChatService")
public class CoreChatServiceImpl implements ChatService {


    @Override
    public Flux<Object> sendMessage(ChatRequest request) {
      return null;
    }


}
