package edu.zsc.ai.domain.service.agent.impl;

import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.ChatService;
import edu.zsc.ai.domain.service.agent.ChatSession;
import edu.zsc.ai.domain.service.agent.ChatSessionFactory;
import edu.zsc.ai.domain.service.agent.ChatStreamBridge;
import edu.zsc.ai.domain.service.agent.multi.MultiAgentChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionFactory chatSessionFactory;
    private final ChatStreamBridge chatStreamBridge;
    private final MultiAgentChatService multiAgentChatService;

    @Override
    public Flux<ChatResponseBlock> chat(ChatRequest request) {
        ChatSession session = chatSessionFactory.create(request);

        if (session.agentMode() == AgentModeEnum.MULTI_AGENT) {
            return multiAgentChatService.chat(request, session);
        }

        AtomicBoolean enterPlanTriggered = new AtomicBoolean(false);

        Flux<ChatResponseBlock> agentFlux = chatStreamBridge.bridge(session, enterPlanTriggered, false);

        return agentFlux.concatWith(Flux.defer(() -> {
            if (!enterPlanTriggered.get()) {
                return Flux.just(ChatResponseBlock.doneBlock());
            }

            log.info("enterPlanMode triggered for conversation {}, chaining Plan mode agent",
                    session.conversationId());

            ChatSession planSession = chatSessionFactory.createPlanContinuation(session, request);
            return chatStreamBridge.bridge(planSession, new AtomicBoolean(false), true);
        })).map(block -> {
            if (Objects.nonNull(block) && Objects.isNull(block.getConversationId())) {
                block.setConversationId(session.conversationId());
            }
            return block;
        });
    }
}
