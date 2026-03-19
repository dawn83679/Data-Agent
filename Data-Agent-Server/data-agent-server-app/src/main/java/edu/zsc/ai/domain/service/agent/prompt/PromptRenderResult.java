package edu.zsc.ai.domain.service.agent.prompt;

import java.util.Map;

public record PromptRenderResult<S>(
        String renderedPrompt,
        Map<S, PromptSectionResult<S>> sectionPayloads,
        int estimatedTokens,
        String debugSummary
) {
}
