package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import edu.zsc.ai.common.constant.MemoryRecallLogConstant;
import edu.zsc.ai.common.constant.MemoryConstant;
import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;

final class MemoryPromptProjectionSupport {

    private static final List<String> SCOPE_TARGET_KEYWORDS = List.of(
            "connection", "connectionid", "connectionname",
            "catalog", "catalogname",
            "schema", "schemaname",
            "database", "dbname",
            "table", "view", "object",
            "连接", "数据库", "库", "表", "视图", "对象"
    );

    private static final List<String> SCOPE_DIRECTIVE_KEYWORDS = List.of(
            "use", "using", "prefer", "avoid", "scope", "stored in", "located in", "rather than", "instead of",
            "查询", "使用", "应使用", "不要", "避免", "优先", "范围", "存储在", "位于", "而非"
    );

    private static final List<String> SEMANTIC_EXECUTION_PATHS = List.of(
            MemoryRecallLogConstant.EXECUTION_PATH_SEMANTIC,
            MemoryRecallLogConstant.EXECUTION_PATH_HYBRID_SEMANTIC
    );

    private MemoryPromptProjectionSupport() {
    }

    static boolean isPreferenceMemory(MemoryRecallItem memory) {
        MemorySubTypeEnum subType = MemorySubTypeEnum.fromCode(memory.getSubType());
        return subType == MemorySubTypeEnum.RESPONSE_FORMAT
                || subType == MemorySubTypeEnum.LANGUAGE_PREFERENCE;
    }

    static boolean isScopeHintMemory(MemoryRecallItem memory) {
        if (memory == null || isPreferenceMemory(memory)) {
            return false;
        }
        String normalized = StringUtils.defaultString(memory.getContent()).toLowerCase(Locale.ROOT);
        boolean mentionsScopeTarget = SCOPE_TARGET_KEYWORDS.stream().anyMatch(normalized::contains);
        boolean containsDirective = SCOPE_DIRECTIVE_KEYWORDS.stream().anyMatch(normalized::contains);
        return mentionsScopeTarget && containsDirective;
    }

    static boolean isPromptInjectableNonPreferenceMemory(MemoryRecallItem memory) {
        if (memory == null || isPreferenceMemory(memory)) {
            return false;
        }
        if (memory.isUsedFallback()) {
            return MemoryScopeEnum.CONVERSATION.matches(memory.getScope());
        }
        return SEMANTIC_EXECUTION_PATHS.contains(StringUtils.defaultString(memory.getExecutionPath()));
    }

    static String renderMemoryList(List<MemoryRecallItem> memories) {
        if (memories == null || memories.isEmpty()) {
            return UserPromptBlockSupport.renderBullets(List.of(PromptConstant.NONE));
        }
        return UserPromptBlockSupport.renderBullets(memories.stream()
                .map(MemoryPromptProjectionSupport::renderMemoryLine)
                .toList());
    }

    static String renderScopeHintList(List<MemoryRecallItem> memories) {
        if (memories == null || memories.isEmpty()) {
            return UserPromptBlockSupport.renderBullets(List.of(PromptConstant.NONE));
        }
        return UserPromptBlockSupport.renderBullets(memories.stream()
                .map(MemoryPromptProjectionSupport::renderScopeHintLine)
                .toList());
    }

    private static String renderMemoryLine(MemoryRecallItem memory) {
        if (memory == null) {
            return PromptConstant.NONE;
        }
        String scope = StringUtils.defaultIfBlank(memory.getScope(), MemoryConstant.DEFAULT_SCOPE);
        String type = StringUtils.defaultString(memory.getMemoryType());
        String subType = StringUtils.defaultString(memory.getSubType());
        if (StringUtils.isBlank(type) && StringUtils.isBlank(subType)) {
            return memory.getContent();
        }
        StringBuilder label = new StringBuilder("[");
        label.append(scope);
        if (StringUtils.isNotBlank(type)) {
            label.append(" · ").append(type);
            if (StringUtils.isNotBlank(subType)) {
                label.append('/').append(subType);
            }
        }
        label.append("] ");
        return label + StringUtils.defaultString(memory.getContent());
    }

    private static String renderScopeHintLine(MemoryRecallItem memory) {
        if (memory == null) {
            return PromptConstant.NONE;
        }
        return StringUtils.defaultIfBlank(StringUtils.trim(memory.getContent()), PromptConstant.NONE);
    }
}
