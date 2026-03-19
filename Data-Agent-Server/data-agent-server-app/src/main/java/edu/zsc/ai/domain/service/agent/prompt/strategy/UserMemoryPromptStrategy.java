package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.common.constant.MemoryConstant;
import edu.zsc.ai.common.enums.ai.MemoryReviewStateEnum;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
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
        List<MemoryRecallItem> memories = context.getMemoryPromptContext().getMemories();
        if (memories.isEmpty()) {
            return PromptConstant.NONE;
        }

        StringBuilder builder = new StringBuilder();
        for (MemoryRecallItem memory : memories) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("- [")
                    .append(PromptTextUtil.escape(resolveMemoryLabel(memory)))
                    .append("] ")
                    .append(PromptTextUtil.escape(memory.getContent()));
        }
        return builder.toString();
    }

    private String resolveMemoryLabel(MemoryRecallItem memory) {
        String scope = memory.getScope() == null ? MemoryConstant.DEFAULT_SCOPE : memory.getScope();
        String type = memory.getMemoryType();
        String subType = memory.getSubType();
        StringBuilder label = new StringBuilder(scope);
        if (MemoryScopeEnum.WORKSPACE.matches(scope) && memory.getWorkspaceLevel() != null) {
            label.append('/').append(memory.getWorkspaceLevel());
        }
        if (type != null && !type.isBlank()) {
            label.append(" · ").append(type);
            if (subType != null && !subType.isBlank()) {
                label.append('/').append(subType);
            }
        }
        if (MemoryReviewStateEnum.NEEDS_REVIEW.getCode().equalsIgnoreCase(memory.getReviewState())) {
            label.append(" · ").append(MemoryReviewStateEnum.NEEDS_REVIEW.getCode());
        }
        return label.toString();
    }
}
