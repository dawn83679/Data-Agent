package edu.zsc.ai.domain.service.ai.recall;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.MemoryRecallPlanningConstant;
import edu.zsc.ai.common.enums.ai.MemorySubTypeEnum;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemoryTypeEnum;

@Component
public class MemoryRecallQueryPlanner {

    private final MemoryRecallPlanningRules planningRules;

    public MemoryRecallQueryPlanner(MemoryRecallPlanningRules planningRules) {
        this.planningRules = planningRules;
    }

    public List<MemoryRecallQuery> plan(MemoryRecallContext context) {
        if (context == null) {
            return List.of();
        }

        MemoryTypeEnum memoryType = resolveMemoryType(context);
        MemorySubTypeEnum subType = resolveMemorySubType(context);
        MemoryRecallPlanningRule rule = planningRules.resolveRule(memoryType);

        if (StringUtils.isNotBlank(context.getScope())) {
            MemoryScopeEnum targetScope = MemoryScopeEnum.fromCode(context.getScope());
            if (targetScope == null) {
                throw new IllegalArgumentException(
                        "Unsupported scope '" + context.getScope() + "'. Valid values: " + MemoryScopeEnum.validCodes());
            }
            return List.of(toQuery(context, targetScope, rule.strategyFor(targetScope), 0,
                    MemoryRecallPlanningConstant.REASON_EXPLICIT_SCOPE));
        }

        MemoryScopeEnum preferredScope = planningRules.preferredScope(subType);
        List<MemoryScopeEnum> scopes = reorderScopes(rule.scopes(), preferredScope);
        String reorderedReason = preferredScope == null || !scopes.contains(preferredScope)
                ? null
                : reorderReason(preferredScope);
        List<MemoryRecallQuery> queries = new ArrayList<>(scopes.size());
        for (int index = 0; index < scopes.size(); index++) {
            MemoryScopeEnum scope = scopes.get(index);
            String reason = index == 0 && reorderedReason != null ? reorderedReason : rule.ruleName();
            queries.add(toQuery(context, scope, rule.strategyFor(scope), index, reason));
        }
        return queries;
    }

    private List<MemoryScopeEnum> reorderScopes(List<MemoryScopeEnum> scopes, MemoryScopeEnum preferredScope) {
        if (preferredScope == null) {
            return scopes;
        }
        if (!scopes.contains(preferredScope)) {
            return scopes;
        }
        List<MemoryScopeEnum> reordered = new ArrayList<>(scopes.size());
        reordered.add(preferredScope);
        scopes.stream()
                .filter(scope -> scope != preferredScope)
                .forEach(reordered::add);
        return reordered;
    }

    private MemoryTypeEnum resolveMemoryType(MemoryRecallContext context) {
        MemoryTypeEnum memoryType = MemoryTypeEnum.fromCode(context.getMemoryType());
        if (memoryType != null) {
            return memoryType;
        }
        MemorySubTypeEnum subType = resolveMemorySubType(context);
        return subType == null ? null : subType.getMemoryType();
    }

    private MemorySubTypeEnum resolveMemorySubType(MemoryRecallContext context) {
        return MemorySubTypeEnum.fromCode(context.getSubType());
    }

    private MemoryRecallQuery toQuery(MemoryRecallContext context,
                                      MemoryScopeEnum scope,
                                      MemoryRecallQueryStrategy strategy,
                                      int priority,
                                      String planningReason) {
        return new MemoryRecallQuery(
                buildQueryName(scope, context.getMemoryType(), context.getSubType(), strategy),
                planningReason,
                scope.getCode(),
                context.getConversationId(),
                context.getQueryText(),
                context.getMemoryType(),
                context.getSubType(),
                context.getMinScore(),
                context.getRecallMode(),
                strategy,
                priority);
    }

    private String buildQueryName(MemoryScopeEnum scope,
                                  String memoryType,
                                  String subType,
                                  MemoryRecallQueryStrategy strategy) {
        StringBuilder builder = new StringBuilder(MemoryRecallPlanningConstant.QUERY_NAME_PREFIX);
        builder.append(':').append(scope.getCode().toLowerCase());
        if (StringUtils.isNotBlank(memoryType)) {
            builder.append(':').append(memoryType.trim().toLowerCase());
        }
        if (StringUtils.isNotBlank(subType)) {
            builder.append(':').append(subType.trim().toLowerCase());
        }
        builder.append(':').append(strategy.name().toLowerCase());
        return builder.toString();
    }

    private String reorderReason(MemoryScopeEnum preferredScope) {
        return switch (preferredScope) {
            case USER -> MemoryRecallPlanningConstant.REASON_SUBTYPE_REORDERED_TO_USER;
            case CONVERSATION -> MemoryRecallPlanningConstant.REASON_SUBTYPE_REORDERED_TO_CONVERSATION;
        };
    }
}
