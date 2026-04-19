package edu.zsc.ai.common.enums.ai;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemorySubTypeEnum {

    RESPONSE_FORMAT("RESPONSE_FORMAT", MemoryTypeEnum.PREFERENCE, false),
    LANGUAGE_PREFERENCE("LANGUAGE_PREFERENCE", MemoryTypeEnum.PREFERENCE, false),

    PRODUCT_RULE("PRODUCT_RULE", MemoryTypeEnum.BUSINESS_RULE, false),
    DOMAIN_RULE("DOMAIN_RULE", MemoryTypeEnum.BUSINESS_RULE, false),
    GOVERNANCE_RULE("GOVERNANCE_RULE", MemoryTypeEnum.BUSINESS_RULE, false),
    SAFETY_RULE("SAFETY_RULE", MemoryTypeEnum.BUSINESS_RULE, false),

    ARCHITECTURE_KNOWLEDGE("ARCHITECTURE_KNOWLEDGE", MemoryTypeEnum.KNOWLEDGE_POINT, false),
    DOMAIN_KNOWLEDGE("DOMAIN_KNOWLEDGE", MemoryTypeEnum.KNOWLEDGE_POINT, false),
    GLOSSARY("GLOSSARY", MemoryTypeEnum.KNOWLEDGE_POINT, false),
    OBJECT_KNOWLEDGE("OBJECT_KNOWLEDGE", MemoryTypeEnum.KNOWLEDGE_POINT, false),

    PROCESS_RULE("PROCESS_RULE", MemoryTypeEnum.WORKFLOW_CONSTRAINT, false),
    APPROVAL_RULE("APPROVAL_RULE", MemoryTypeEnum.WORKFLOW_CONSTRAINT, false),
    IMPLEMENTATION_CONSTRAINT("IMPLEMENTATION_CONSTRAINT", MemoryTypeEnum.WORKFLOW_CONSTRAINT, false),
    REVIEW_CONSTRAINT("REVIEW_CONSTRAINT", MemoryTypeEnum.WORKFLOW_CONSTRAINT, false),
    CONVERSATION_WORKING_MEMORY("CONVERSATION_WORKING_MEMORY", MemoryTypeEnum.WORKFLOW_CONSTRAINT, true),

    QUERY_PATTERN("QUERY_PATTERN", MemoryTypeEnum.GOLDEN_SQL_CASE, false),
    JOIN_STRATEGY("JOIN_STRATEGY", MemoryTypeEnum.GOLDEN_SQL_CASE, false),
    VALIDATED_SQL("VALIDATED_SQL", MemoryTypeEnum.GOLDEN_SQL_CASE, false),
    METRIC_CALCULATION("METRIC_CALCULATION", MemoryTypeEnum.GOLDEN_SQL_CASE, false);

    private final String code;
    private final MemoryTypeEnum memoryType;
    private final boolean internalOnly;

    public static MemorySubTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(value -> value.code.equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public static List<String> validCodesFor(MemoryTypeEnum memoryType) {
        if (memoryType == null) {
            return List.of();
        }
        return Arrays.stream(values())
                .filter(value -> value.memoryType == memoryType)
                .filter(value -> !value.internalOnly)
                .map(MemorySubTypeEnum::getCode)
                .toList();
    }

    public static String validCodesForText(MemoryTypeEnum memoryType) {
        return String.join(", ", validCodesFor(memoryType));
    }

    public static String validCodes() {
        return Arrays.stream(values())
                .filter(value -> !value.internalOnly)
                .map(MemorySubTypeEnum::getCode)
                .collect(Collectors.joining(", "));
    }

    public boolean belongsTo(MemoryTypeEnum memoryType) {
        return this.memoryType == memoryType;
    }
}
