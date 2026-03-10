package edu.zsc.ai.domain.service.agent.multi;

import edu.zsc.ai.api.model.request.ChatRequest;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.ChatSession;
import edu.zsc.ai.domain.service.agent.ChatSessionFactory;
import edu.zsc.ai.domain.service.agent.ChatStreamBridge;
import edu.zsc.ai.domain.service.agent.multi.model.MultiAgentRun;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class MultiAgentChatService {

    private final ChatSessionFactory chatSessionFactory;
    private final ChatStreamBridge chatStreamBridge;
    private final MultiAgentRunStore multiAgentRunStore;

    public Flux<ChatResponseBlock> chat(ChatRequest request, ChatSession session) {
        MultiAgentRun run = multiAgentRunStore.createRun(
                session.conversationId(),
                session.contextSnapshot().getUserId(),
                request.getMessage());

        RequestContextInfo orchestratorContext = RequestContextInfo.builder()
                .conversationId(session.contextSnapshot().getConversationId())
                .userId(session.contextSnapshot().getUserId())
                .connectionId(session.contextSnapshot().getConnectionId())
                .catalog(session.contextSnapshot().getCatalog())
                .schema(session.contextSnapshot().getSchema())
                .modelName(session.contextSnapshot().getModelName())
                .language(session.contextSnapshot().getLanguage())
                .agentMode(AgentModeEnum.MULTI_AGENT.getCode())
                .runId(run.getRunId())
                .agentRole(AgentRoleEnum.ORCHESTRATOR.getCode())
                .build();

        ChatSession orchestratorSession = chatSessionFactory.createContinuation(
                session,
                request.getLanguage(),
                session.enrichedMessage(),
                AgentModeEnum.MULTI_AGENT,
                orchestratorContext);

        return chatStreamBridge.bridge(orchestratorSession, new AtomicBoolean(false), true)
                .doOnComplete(() -> multiAgentRunStore.completeRun(run.getRunId(), "COMPLETED"))
                .doOnError(error -> multiAgentRunStore.completeRun(run.getRunId(), "FAILED"))
                .map(block -> {
                    if (block != null && block.getConversationId() == null) {
                        block.setConversationId(session.conversationId());
                    }
                    return block;
                });
    }
}
