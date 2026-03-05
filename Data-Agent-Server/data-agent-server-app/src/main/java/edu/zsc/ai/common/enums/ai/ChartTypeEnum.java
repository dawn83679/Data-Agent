package edu.zsc.ai.common.enums.ai;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

/**
 * Supported chart types for the built-in renderChart tool.
 */
public enum ChartTypeEnum {

    LINE,
    BAR,
    PIE,
    SCATTER,
    AREA;

    public static ChartTypeEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Chart type cannot be blank");
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(v -> v.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported chart type: " + value));
    }
}
