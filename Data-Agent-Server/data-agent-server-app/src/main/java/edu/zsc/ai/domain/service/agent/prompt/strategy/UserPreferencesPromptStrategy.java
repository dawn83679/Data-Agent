package edu.zsc.ai.domain.service.agent.prompt.strategy;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;

@Component
public class UserPreferencesPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.RESPONSE_PREFERENCES;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        java.util.List<String> lines = context.getMemoryPromptContext().getMemories().stream()
                .filter(MemoryPromptProjectionSupport::isPreferenceMemory)
                .map(this::renderPreferenceLine)
                .toList();
        if (lines.isEmpty()) {
            return UserPromptBlockSupport.renderBlock(
                    context,
                    "请默认遵循以下偏好：",
                    "Apply the following preferences by default:",
                    java.util.List.of(PromptConstant.NONE));
        }
        return UserPromptBlockSupport.renderBlock(
                context,
                "请默认遵循以下偏好：",
                "Apply the following preferences by default:",
                lines);
    }

    private String renderPreferenceLine(edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem memory) {
        return memory.getContent();
    }
}
