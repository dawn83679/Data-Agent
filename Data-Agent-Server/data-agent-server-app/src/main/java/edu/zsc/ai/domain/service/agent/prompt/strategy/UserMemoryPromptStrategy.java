package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.MemoryConstant;
import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;

@Component
public class UserMemoryPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.USER_MEMORY;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        List<MemoryRecallItem> contextMemories = context.getMemoryPromptContext().getMemories().stream()
                .filter(memory -> !isPreferenceMemory(memory))
                .toList();
        if (contextMemories.isEmpty()) {
            return PromptConstant.NONE;
        }
        return renderMemoryList(contextMemories);
    }

    private String renderMemoryList(List<MemoryRecallItem> memories) {
        return memories.stream()
                .map(memory -> "- ["
                        + PromptTextUtil.escape(resolveMemoryLabel(memory))
                        + "] "
                        + PromptTextUtil.escape(memory.getContent()))
                .collect(Collectors.joining("\n"));
    }

    private String resolveMemoryLabel(MemoryRecallItem memory) {
        String scope = memory.getScope() == null ? MemoryConstant.DEFAULT_SCOPE : memory.getScope();
        String type = memory.getMemoryType();
        String subType = memory.getSubType();
        StringBuilder label = new StringBuilder(scope);
        if (type != null && !type.isBlank()) {
            label.append(" · ").append(type);
            if (subType != null && !subType.isBlank()) {
                label.append('/').append(subType);
            }
        }
        return label.toString();
    }

    private boolean isPreferenceMemory(MemoryRecallItem memory) {
        MemorySubTypeEnum subType = MemorySubTypeEnum.fromCode(memory.getSubType());
        return subType == MemorySubTypeEnum.RESPONSE_FORMAT
                || subType == MemorySubTypeEnum.LANGUAGE_PREFERENCE;
    }
}
