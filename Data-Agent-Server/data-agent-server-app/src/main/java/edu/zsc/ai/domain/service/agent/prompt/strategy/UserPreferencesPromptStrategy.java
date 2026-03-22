package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;

@Component
public class UserPreferencesPromptStrategy extends AbstractUserPromptHandler {

    private static final String LANGUAGE_LABEL = "Preferred response language: ";
    private static final String FORMAT_LABEL = "Preferred response format: ";

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.USER_PREFERENCES;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        List<MemoryRecallItem> preferenceMemories = context.getMemoryPromptContext().getMemories().stream()
                .filter(this::isPreferenceMemory)
                .toList();
        if (preferenceMemories.isEmpty()) {
            return PromptConstant.NONE;
        }
        return preferenceMemories.stream()
                .map(this::renderPreferenceLine)
                .collect(Collectors.joining("\n"));
    }

    private String renderPreferenceLine(MemoryRecallItem memory) {
        MemorySubTypeEnum subType = MemorySubTypeEnum.fromCode(memory.getSubType());
        String label = switch (subType) {
            case LANGUAGE_PREFERENCE -> LANGUAGE_LABEL;
            case RESPONSE_FORMAT -> FORMAT_LABEL;
            default -> "";
        };
        return label + PromptTextUtil.escape(memory.getContent());
    }

    private boolean isPreferenceMemory(MemoryRecallItem memory) {
        MemorySubTypeEnum subType = MemorySubTypeEnum.fromCode(memory.getSubType());
        return subType == MemorySubTypeEnum.LANGUAGE_PREFERENCE
                || subType == MemorySubTypeEnum.RESPONSE_FORMAT;
    }
}
