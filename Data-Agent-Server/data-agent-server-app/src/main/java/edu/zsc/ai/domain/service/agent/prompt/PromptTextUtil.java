package edu.zsc.ai.domain.service.agent.prompt;

import org.apache.commons.lang3.StringUtils;

public final class PromptTextUtil {

    private PromptTextUtil() {
    }

    public static String escape(String raw) {
        String value = StringUtils.defaultString(raw);
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static String truncate(String value, int maxChars) {
        String normalized = StringUtils.normalizeSpace(StringUtils.defaultString(value));
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxChars - 3)) + "...";
    }
}
