package edu.zsc.ai.domain.service.agent.prompt;

import java.util.Map;

public record PromptSectionResult<S>(
        S section,
        String content,
        boolean rendered,
        Map<String, Object> metadata
) {
}
