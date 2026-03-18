package edu.zsc.ai.observability.config;

import edu.zsc.ai.config.ai.AgentObservabilityProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAgentObservabilityConfigProvider implements AgentObservabilityConfigProvider {

    private final AgentObservabilityProperties properties;
    private final AgentObservabilityRuntimeState runtimeState;

    @Override
    public AgentObservabilitySettings current() {
        AgentObservabilitySettings configured = configuredSettings();
        AgentObservabilitySettingsOverride override = runtimeState.get();
        return merge(configured, override);
    }

    @Override
    public AgentObservabilitySnapshot snapshot() {
        AgentObservabilitySettings configured = configuredSettings();
        AgentObservabilitySettingsOverride override = runtimeState.get();
        return AgentObservabilitySnapshot.builder()
                .configured(configured)
                .runtimeOverride(override)
                .effective(merge(configured, override))
                .build();
    }

    private AgentObservabilitySettings configuredSettings() {
        return AgentObservabilitySettings.builder()
                .enabled(properties.isEnabled())
                .runtimeLogEnabled(properties.isRuntimeLogEnabled())
                .consoleLogEnabled(properties.isConsoleLogEnabled())
                .sseEventLogEnabled(properties.isSseEventLogEnabled())
                .modelEventLogEnabled(properties.isModelEventLogEnabled())
                .toolEventLogEnabled(properties.isToolEventLogEnabled())
                .includePrompt(properties.isIncludePrompt())
                .includeResponse(properties.isIncludeResponse())
                .includeTokenStream(properties.isIncludeTokenStream())
                .runtimeLogDir(properties.getRuntimeLogDir())
                .build();
    }

    private AgentObservabilitySettings merge(AgentObservabilitySettings configured,
                                             AgentObservabilitySettingsOverride override) {
        return AgentObservabilitySettings.builder()
                .enabled(booleanValue(override.getEnabled(), configured.isEnabled()))
                .runtimeLogEnabled(booleanValue(override.getRuntimeLogEnabled(), configured.isRuntimeLogEnabled()))
                .consoleLogEnabled(booleanValue(override.getConsoleLogEnabled(), configured.isConsoleLogEnabled()))
                .sseEventLogEnabled(booleanValue(override.getSseEventLogEnabled(), configured.isSseEventLogEnabled()))
                .modelEventLogEnabled(booleanValue(override.getModelEventLogEnabled(), configured.isModelEventLogEnabled()))
                .toolEventLogEnabled(booleanValue(override.getToolEventLogEnabled(), configured.isToolEventLogEnabled()))
                .includePrompt(booleanValue(override.getIncludePrompt(), configured.isIncludePrompt()))
                .includeResponse(booleanValue(override.getIncludeResponse(), configured.isIncludeResponse()))
                .includeTokenStream(booleanValue(override.getIncludeTokenStream(), configured.isIncludeTokenStream()))
                .runtimeLogDir(StringUtils.defaultIfBlank(override.getRuntimeLogDir(), configured.getRuntimeLogDir()))
                .build();
    }

    private boolean booleanValue(Boolean override, boolean fallback) {
        return override != null ? override : fallback;
    }
}
