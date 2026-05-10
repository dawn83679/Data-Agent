package edu.zsc.ai.common.enums.ai;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SkillEnum {

    CHART(
            "chart",
            "skills/chart/SKILL.md",
            "为最终结构化图表输出提供图表渲染指导。"
    );

    private final String skillName;
    private final String resourcePath;
    private final String description;

    private static final Map<String, SkillEnum> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(SkillEnum::getSkillName, e -> e));

    public static SkillEnum fromName(String name) {
        return name == null ? null : BY_NAME.get(name.trim().toLowerCase());
    }

    public static String validNames() {
        return BY_NAME.keySet().stream().sorted().collect(Collectors.joining(", "));
    }
}
