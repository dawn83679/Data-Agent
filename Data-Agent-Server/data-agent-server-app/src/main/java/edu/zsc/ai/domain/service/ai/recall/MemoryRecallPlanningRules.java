package edu.zsc.ai.domain.service.ai.recall;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.MemoryRecallPlanningConstant;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;

@Component
public class MemoryRecallPlanningRules {

    public MemoryRecallPlanningRule resolveRule(MemoryTypeEnum memoryType, MemoryRecallMode recallMode) {
        boolean promptMode = recallMode == MemoryRecallMode.PROMPT;
        if (memoryType == null) {
            return new MemoryRecallPlanningRule(
                    MemoryRecallPlanningConstant.REASON_FALLBACK_DEFAULT_SCOPE_PLAN,
                    List.of(MemoryScopeEnum.CONVERSATION, MemoryScopeEnum.USER),
                    MemoryRecallQueryStrategy.HYBRID,
                    promptMode ? MemoryRecallQueryStrategy.HYBRID : MemoryRecallQueryStrategy.BROWSE);
        }

        return switch (memoryType) {
            case PREFERENCE -> new MemoryRecallPlanningRule(
                    MemoryRecallPlanningConstant.REASON_MEMORY_TYPE_PREFERENCE_DEFAULT,
                    List.of(MemoryScopeEnum.USER, MemoryScopeEnum.CONVERSATION),
                    MemoryRecallQueryStrategy.BROWSE,
                    MemoryRecallQueryStrategy.BROWSE);
            case BUSINESS_RULE -> new MemoryRecallPlanningRule(
                    MemoryRecallPlanningConstant.REASON_MEMORY_TYPE_BUSINESS_RULE_DEFAULT,
                    List.of(MemoryScopeEnum.USER, MemoryScopeEnum.CONVERSATION),
                    MemoryRecallQueryStrategy.HYBRID,
                    promptMode ? MemoryRecallQueryStrategy.HYBRID : MemoryRecallQueryStrategy.BROWSE);
            case KNOWLEDGE_POINT -> new MemoryRecallPlanningRule(
                    MemoryRecallPlanningConstant.REASON_MEMORY_TYPE_KNOWLEDGE_POINT_DEFAULT,
                    List.of(MemoryScopeEnum.USER, MemoryScopeEnum.CONVERSATION),
                    MemoryRecallQueryStrategy.HYBRID,
                    promptMode ? MemoryRecallQueryStrategy.HYBRID : MemoryRecallQueryStrategy.BROWSE);
            case GOLDEN_SQL_CASE -> new MemoryRecallPlanningRule(
                    MemoryRecallPlanningConstant.REASON_MEMORY_TYPE_GOLDEN_SQL_CASE_DEFAULT,
                    List.of(MemoryScopeEnum.USER, MemoryScopeEnum.CONVERSATION),
                    MemoryRecallQueryStrategy.HYBRID,
                    promptMode ? MemoryRecallQueryStrategy.HYBRID : MemoryRecallQueryStrategy.BROWSE);
            case WORKFLOW_CONSTRAINT -> new MemoryRecallPlanningRule(
                    MemoryRecallPlanningConstant.REASON_MEMORY_TYPE_WORKFLOW_CONSTRAINT_DEFAULT,
                    List.of(MemoryScopeEnum.CONVERSATION, MemoryScopeEnum.USER),
                    MemoryRecallQueryStrategy.HYBRID,
                    promptMode ? MemoryRecallQueryStrategy.HYBRID : MemoryRecallQueryStrategy.BROWSE);
        };
    }

    public MemoryScopeEnum preferredScope(MemorySubTypeEnum subType) {
        if (subType == null) {
            return null;
        }
        return switch (subType) {
            case RESPONSE_FORMAT, LANGUAGE_PREFERENCE -> MemoryScopeEnum.USER;
            case PRODUCT_RULE, DOMAIN_RULE, GOVERNANCE_RULE, SAFETY_RULE,
                    ARCHITECTURE_KNOWLEDGE, DOMAIN_KNOWLEDGE, OBJECT_KNOWLEDGE,
                    QUERY_PATTERN, JOIN_STRATEGY, VALIDATED_SQL, METRIC_CALCULATION,
                    GLOSSARY -> MemoryScopeEnum.USER;
            case PROCESS_RULE, APPROVAL_RULE, IMPLEMENTATION_CONSTRAINT, REVIEW_CONSTRAINT,
                    CONVERSATION_WORKING_MEMORY -> MemoryScopeEnum.CONVERSATION;
        };
    }
}
