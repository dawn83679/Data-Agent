package edu.zsc.ai.agent.subagent;

import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Shared formatting and error helpers for sub-agent implementations and orchestrator tools.
 */
public final class SubAgentTextSupport {

    private static final int PREVIEW_LENGTH = 160;

    private SubAgentTextSupport() {
    }

    public static String preview(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return StringUtils.abbreviate(StringUtils.normalizeSpace(value), PREVIEW_LENGTH);
    }

    public static String summarizeObjects(List<ExploreObject> objects) {
        if (CollectionUtils.isEmpty(objects)) {
            return "[]";
        }
        List<String> names = new ArrayList<>();
        for (ExploreObject object : objects.stream().limit(3).toList()) {
            names.add(qualifiedName(object));
        }
        return names.toString();
    }

    public static String qualifiedName(ExploreObject object) {
        List<String> parts = new ArrayList<>();
        if (object == null) {
            return "";
        }
        if (StringUtils.isNotBlank(object.getCatalog())) {
            parts.add(object.getCatalog());
        }
        if (StringUtils.isNotBlank(object.getSchema())) {
            parts.add(object.getSchema());
        }
        if (StringUtils.isNotBlank(object.getObjectName())) {
            parts.add(object.getObjectName());
        }
        return String.join(".", parts);
    }

    public static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current != null ? current : throwable;
    }

    public static String rootCauseMessage(Throwable throwable) {
        Throwable cause = rootCause(throwable);
        return cause == null ? null : StringUtils.defaultIfBlank(cause.getMessage(), cause.getClass().getSimpleName());
    }

    public static String errorSummary(Throwable throwable, String fallbackMessage) {
        return errorSummary(throwable, fallbackMessage, null);
    }

    public static String errorSummary(Throwable throwable, String fallbackMessage, Long timeoutSeconds) {
        String fallback = StringUtils.defaultIfBlank(fallbackMessage, "SubAgent execution failed");
        Throwable cause = rootCause(throwable);
        if (cause == null) {
            return fallback;
        }
        if (cause instanceof TimeoutException) {
            return StringUtils.defaultIfBlank(
                    cause.getMessage(),
                    timeoutSeconds != null && timeoutSeconds > 0
                            ? "Timed out after " + timeoutSeconds + "s"
                            : "Timed out"
            );
        }
        return StringUtils.defaultIfBlank(
                StringUtils.defaultIfBlank(cause.getMessage(), cause.getClass().getSimpleName()),
                fallback
        );
    }
}
