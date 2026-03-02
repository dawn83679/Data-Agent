package edu.zsc.ai.domain.service.agent.model;

import dev.langchain4j.invocation.InvocationParameters;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Unified command object for agent chat invocation.
 * imageUrls is reserved for upcoming multimodal support.
 */
@Getter
@Builder
public class AgentChatCommand {

    private final String memoryId;
    private final String text;
    private final InvocationParameters parameters;

    @Builder.Default
    private final List<String> imageUrls = List.of();
}
