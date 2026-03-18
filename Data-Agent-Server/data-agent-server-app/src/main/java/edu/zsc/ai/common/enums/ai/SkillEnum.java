package edu.zsc.ai.common.enums.ai;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Available skills that can be loaded via the activateSkill tool.
 */
@Getter
@RequiredArgsConstructor
public enum SkillEnum {

    CHART("chart", "skills/chart.md"),
    SQL_OPTIMIZATION("sql-optimization", "skills/sql-optimization.md");

    private final String skillName;
    private final String resourcePath;

    private static final Map<String, SkillEnum> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(SkillEnum::getSkillName, e -> e));

    public static SkillEnum fromName(String name) {
        return name == null ? null : BY_NAME.get(name.trim().toLowerCase());
    }

    public static String validNames() {
        return BY_NAME.keySet().stream().sorted().collect(Collectors.joining(", "));
    }
}
