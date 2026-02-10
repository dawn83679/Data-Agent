package edu.zsc.ai.domain.service.agent;

import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.model.request.ChatRequest;
import edu.zsc.ai.model.request.SubmitToolAnswerRequest;
import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<ChatResponseBlock> chat(ChatRequest request);

    /**
     * Replaces the askUserQuestion tool result with the user's answer in memory,
     * then continues the conversation (streaming). The continue user message is filtered
     * from persistence via {@link edu.zsc.ai.common.constant.HitlConstants#HITL_CONTINUE_MESSAGE_PREFIX}.
     */
    Flux<ChatResponseBlock> submitToolAnswerAndContinue(SubmitToolAnswerRequest request);
}
