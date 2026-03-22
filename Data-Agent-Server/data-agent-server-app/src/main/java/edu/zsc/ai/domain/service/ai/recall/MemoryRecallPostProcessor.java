package edu.zsc.ai.domain.service.ai.recall;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.MemoryRecallConstant;
import edu.zsc.ai.common.constant.MemoryConstant;
import edu.zsc.ai.common.enums.ai.MemoryScopeEnum;
import edu.zsc.ai.common.enums.ai.MemoryWorkspaceLevelEnum;

@Component
public class MemoryRecallPostProcessor {

    public MemoryRecallResult process(MemoryRecallContext context, List<MemoryRecallItem> items) {
        List<MemoryRecallItem> filtered = items.stream()
                .filter(item -> matchesFilter(context.getMemoryType(), item.getMemoryType()))
                .filter(item -> matchesFilter(context.getSubType(), item.getSubType()))
                .toList();

        Map<String, MemoryRecallItem> deduplicated = new LinkedHashMap<>();
        for (MemoryRecallItem item : filtered) {
            String key = dedupKey(item);
            MemoryRecallItem existing = deduplicated.get(key);
            if (existing == null || recallComparator().compare(item, existing) < 0) {
                deduplicated.put(key, item);
            }
        }

        List<MemoryRecallItem> sortedItems = deduplicated.values().stream()
                .sorted(recallComparator())
                .toList();

        return MemoryRecallResult.builder()
                .items(sortedItems)
                .appliedFilters(buildFilters(context))
                .summary(buildSummary(sortedItems))
                .build();
    }

    private boolean matchesFilter(String expected, String actual) {
        return StringUtils.isBlank(expected) || StringUtils.equalsIgnoreCase(expected, actual);
    }

    private Map<String, Object> buildFilters(MemoryRecallContext context) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put(MemoryRecallConstant.FILTER_RECALL_MODE, context.getRecallMode().name());
        if (StringUtils.isNotBlank(context.getQueryText())) {
            filters.put(MemoryRecallConstant.FILTER_INTENT, context.getQueryText());
        }
        if (StringUtils.isNotBlank(context.getScope())) {
            filters.put(MemoryRecallConstant.FILTER_SCOPE, context.getScope());
        }
        if (StringUtils.isNotBlank(context.getMemoryType())) {
            filters.put(MemoryRecallConstant.FILTER_MEMORY_TYPE, context.getMemoryType());
        }
        if (StringUtils.isNotBlank(context.getSubType())) {
            filters.put(MemoryRecallConstant.FILTER_SUB_TYPE, context.getSubType());
        }
        return filters;
    }

    private String buildSummary(List<MemoryRecallItem> items) {
        if (items.isEmpty()) {
            return MemoryRecallConstant.NO_MATCH_SUMMARY;
        }
        StringJoiner scopes = new StringJoiner(", ");
        items.stream()
                .map(item -> StringUtils.defaultIfBlank(item.getScope(), MemoryConstant.DEFAULT_SCOPE))
                .distinct()
                .forEach(scopes::add);
        return MemoryRecallConstant.RECALL_SUMMARY_PREFIX
                + items.size()
                + MemoryRecallConstant.RECALL_SUMMARY_SUFFIX
                + scopes
                + MemoryRecallConstant.RECALL_SUMMARY_END;
    }

    private Comparator<MemoryRecallItem> recallComparator() {
        return Comparator.comparingInt(this::scopePriority)
                .thenComparing(Comparator.comparingInt(this::workspacePriority).reversed())
                .thenComparing(Comparator.comparingDouble(MemoryRecallItem::getScore).reversed())
                .thenComparing(MemoryRecallItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MemoryRecallItem::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int scopePriority(MemoryRecallItem item) {
        MemoryScopeEnum scope = MemoryScopeEnum.fromCode(StringUtils.defaultIfBlank(item.getScope(), MemoryConstant.DEFAULT_SCOPE));
        return scope == null ? Integer.MAX_VALUE : scope.getPriority();
    }

    private int workspacePriority(MemoryRecallItem item) {
        MemoryWorkspaceLevelEnum level = MemoryWorkspaceLevelEnum.fromCode(item.getWorkspaceLevel());
        return level == null ? -1 : level.getPriority();
    }

    private String dedupKey(MemoryRecallItem item) {
        return StringUtils.defaultIfBlank(item.getMemoryType(), "")
                + "|" + StringUtils.defaultIfBlank(item.getSubType(), "")
                + "|" + StringUtils.defaultIfBlank(normalizedContent(item), "");
    }

    private String normalizedContent(MemoryRecallItem item) {
        if (StringUtils.isNotBlank(item.getNormalizedContentKey())) {
            return item.getNormalizedContentKey();
        }
        return StringUtils.normalizeSpace(Objects.toString(item.getContent(), "")).toLowerCase();
    }
}
