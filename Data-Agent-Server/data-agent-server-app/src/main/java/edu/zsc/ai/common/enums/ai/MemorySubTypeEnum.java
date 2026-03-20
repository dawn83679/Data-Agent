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

    RESPONSE_STYLE("RESPONSE_STYLE", MemoryTypeEnum.PREFERENCE),
    OUTPUT_FORMAT("OUTPUT_FORMAT", MemoryTypeEnum.PREFERENCE),
    LANGUAGE_PREFERENCE("LANGUAGE_PREFERENCE", MemoryTypeEnum.PREFERENCE),
    INTERACTION_STYLE("INTERACTION_STYLE", MemoryTypeEnum.PREFERENCE),
    DECISION_STYLE("DECISION_STYLE", MemoryTypeEnum.PREFERENCE),

    PRODUCT_RULE("PRODUCT_RULE", MemoryTypeEnum.BUSINESS_RULE),
    DOMAIN_RULE("DOMAIN_RULE", MemoryTypeEnum.BUSINESS_RULE),
    GOVERNANCE_RULE("GOVERNANCE_RULE", MemoryTypeEnum.BUSINESS_RULE),
    SAFETY_RULE("SAFETY_RULE", MemoryTypeEnum.BUSINESS_RULE),

    ARCHITECTURE_KNOWLEDGE("ARCHITECTURE_KNOWLEDGE", MemoryTypeEnum.KNOWLEDGE_POINT),
    DOMAIN_KNOWLEDGE("DOMAIN_KNOWLEDGE", MemoryTypeEnum.KNOWLEDGE_POINT),
    GLOSSARY("GLOSSARY", MemoryTypeEnum.KNOWLEDGE_POINT),
    OBJECT_KNOWLEDGE("OBJECT_KNOWLEDGE", MemoryTypeEnum.KNOWLEDGE_POINT),

    PROCESS_RULE("PROCESS_RULE", MemoryTypeEnum.WORKFLOW_CONSTRAINT),
    APPROVAL_RULE("APPROVAL_RULE", MemoryTypeEnum.WORKFLOW_CONSTRAINT),
    IMPLEMENTATION_CONSTRAINT("IMPLEMENTATION_CONSTRAINT", MemoryTypeEnum.WORKFLOW_CONSTRAINT),
    REVIEW_CONSTRAINT("REVIEW_CONSTRAINT", MemoryTypeEnum.WORKFLOW_CONSTRAINT),

    QUERY_PATTERN("QUERY_PATTERN", MemoryTypeEnum.GOLDEN_SQL_CASE),
    JOIN_STRATEGY("JOIN_STRATEGY", MemoryTypeEnum.GOLDEN_SQL_CASE),
    VALIDATED_SQL("VALIDATED_SQL", MemoryTypeEnum.GOLDEN_SQL_CASE),
    METRIC_CALCULATION("METRIC_CALCULATION", MemoryTypeEnum.GOLDEN_SQL_CASE);

    private final String code;
    private final MemoryTypeEnum memoryType;

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
                .map(MemorySubTypeEnum::getCode)
                .toList();
    }

    public static String validCodesForText(MemoryTypeEnum memoryType) {
        return String.join(", ", validCodesFor(memoryType));
    }

    public static String validCodes() {
        return Arrays.stream(values())
                .map(MemorySubTypeEnum::getCode)
                .collect(Collectors.joining(", "));
    }

    public boolean belongsTo(MemoryTypeEnum memoryType) {
        return this.memoryType == memoryType;
    }
}
