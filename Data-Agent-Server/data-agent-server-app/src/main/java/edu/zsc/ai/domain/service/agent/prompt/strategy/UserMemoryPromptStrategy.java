package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.common.constant.MemoryConstant;
import edu.zsc.ai.common.constant.UserPromptTagConstant;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;

@Component
public class UserMemoryPromptStrategy extends AbstractUserPromptHandler {

    private static final String PREFERENCE_TAG = "preference";
    private static final String SCOPE_TAG = "scope";
    private static final String MEMORY_TYPE_TAG = "memory_type";
    private static final String SUB_TYPE_TAG = "sub_type";
    private static final String PRIORITY_TAG = "priority";
    private static final String CONTENT_TAG = "content";

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

        List<MemoryRecallItem> preferenceMemories = memories.stream()
                .filter(this::isResponseConstraint)
                .toList();
        List<MemoryRecallItem> contextMemories = memories.stream()
                .filter(memory -> !isResponseConstraint(memory))
                .toList();

        StringBuilder builder = new StringBuilder();

        if (!preferenceMemories.isEmpty()) {
            builder.append(UserPromptTagConstant.USER_PREFERENCES_OPEN)
                    .append('\n')
                    .append(renderPreferenceXml(preferenceMemories))
                    .append('\n')
                    .append(UserPromptTagConstant.USER_PREFERENCES_CLOSE);
        }

        if (!contextMemories.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(UserPromptTagConstant.USER_MEMORY_CONTEXT_OPEN)
                    .append('\n')
                    .append(renderMemoryList(contextMemories))
                    .append('\n')
                    .append(UserPromptTagConstant.USER_MEMORY_CONTEXT_CLOSE);
        }

        return builder.toString();
    }

    private String renderMemoryList(List<MemoryRecallItem> memories) {
        return memories.stream()
                .map(memory -> "- ["
                        + PromptTextUtil.escape(resolveMemoryLabel(memory))
                        + "] "
                        + PromptTextUtil.escape(memory.getContent()))
                .collect(Collectors.joining("\n"));
    }

    private String renderPreferenceXml(List<MemoryRecallItem> memories) {
        return memories.stream()
                .map(this::renderPreferenceXml)
                .collect(Collectors.joining("\n"));
    }

    private String renderPreferenceXml(MemoryRecallItem memory) {
        StringBuilder builder = new StringBuilder();
        builder.append(UserPromptTagConstant.open(PREFERENCE_TAG)).append('\n');
        appendXmlTag(builder, SCOPE_TAG, memory.getScope() == null ? MemoryConstant.DEFAULT_SCOPE : memory.getScope());
        appendXmlTag(builder, MEMORY_TYPE_TAG, memory.getMemoryType());
        appendXmlTag(builder, SUB_TYPE_TAG, memory.getSubType());
        appendXmlTag(builder, PRIORITY_TAG, resolvePriority(memory));
        appendXmlTag(builder, CONTENT_TAG, memory.getContent());
        builder.append(UserPromptTagConstant.close(PREFERENCE_TAG));
        return builder.toString();
    }

    private void appendXmlTag(StringBuilder builder, String tagName, String value) {
        builder.append("  ")
                .append(UserPromptTagConstant.open(tagName))
                .append(PromptTextUtil.escape(value))
                .append(UserPromptTagConstant.close(tagName))
                .append('\n');
    }

    private String resolvePriority(MemoryRecallItem memory) {
        if (isHighPriorityResponseConstraint(memory)) {
            return "HIGH_PRIORITY_RESPONSE_CONSTRAINT";
        }
        if (isResponseConstraint(memory)) {
            return "RESPONSE_CONSTRAINT";
        }
        return "MEMORY";
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
        if (isHighPriorityResponseConstraint(memory)) {
            label.append(" · HIGH_PRIORITY_RESPONSE_CONSTRAINT");
        } else if (isResponseConstraint(memory)) {
            label.append(" · RESPONSE_CONSTRAINT");
        }
        return label.toString();
    }

    private boolean isHighPriorityResponseConstraint(MemoryRecallItem memory) {
        return MemorySubTypeEnum.LANGUAGE_PREFERENCE.getCode().equalsIgnoreCase(memory.getSubType());
    }

    private boolean isResponseConstraint(MemoryRecallItem memory) {
        MemorySubTypeEnum subType = MemorySubTypeEnum.fromCode(memory.getSubType());
        if (subType == null) {
            return false;
        }
        return switch (subType) {
            case RESPONSE_STYLE, OUTPUT_FORMAT, LANGUAGE_PREFERENCE, INTERACTION_STYLE, DECISION_STYLE -> true;
            default -> false;
        };
    }
}
