package edu.zsc.ai.constant;

import java.util.Set;

/**
 * XML tag constants
 */
public class XmlTagConstants {

    public static final String START_TOOL_USE_TAG = "<tool_use>";
    public static final String END_TOOL_USE_TAG = "</tool_use>";
    public static final String START_TOOL_NAME_TAG = "<tool_name>";
    public static final String END_TOOL_NAME_TAG = "</tool_name>";
    public static final String START_TOOL_PARAMS_TAG = "<tool_params>";
    public static final String END_TOOL_PARAMS_TAG = "</tool_params>";

    /**
     * Set of all start tags for quick detection
     */
    private static final Set<String> START_TAGS = Set.of(
            START_TOOL_USE_TAG,
            START_TOOL_NAME_TAG,
            START_TOOL_PARAMS_TAG
    );

    /**
     * Get maximum tag length for buffer judgment
     */
    public static final int MAX_TAG_LENGTH = START_TAGS.stream()
            .mapToInt(String::length)
            .max()
            .orElse(0);

    /**
     * Check if buffer contains valid start tag
     */
    public static boolean containsTag(String buffer) {
        return START_TAGS.stream().anyMatch(buffer::startsWith);
    }

    /**
     * Get all start tags
     */
    public static Set<String> getStartTags() {
        return Set.copyOf(START_TAGS);
    }
}