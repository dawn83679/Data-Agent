package edu.zsc.ai.observability.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class AgentObservabilityRuntimeState {

    private final AtomicReference<AgentObservabilitySettingsOverride> runtimeOverride =
            new AtomicReference<>(new AgentObservabilitySettingsOverride());

    public AgentObservabilitySettingsOverride get() {
        AgentObservabilitySettingsOverride current = runtimeOverride.get();
        return AgentObservabilitySettingsOverride.builder()
                .enabled(current.getEnabled())
                .runtimeLogEnabled(current.getRuntimeLogEnabled())
                .consoleLogEnabled(current.getConsoleLogEnabled())
                .sseEventLogEnabled(current.getSseEventLogEnabled())
                .modelEventLogEnabled(current.getModelEventLogEnabled())
                .toolEventLogEnabled(current.getToolEventLogEnabled())
                .includePrompt(current.getIncludePrompt())
                .includeResponse(current.getIncludeResponse())
                .includeTokenStream(current.getIncludeTokenStream())
                .runtimeLogDir(current.getRuntimeLogDir())
                .build();
    }

    public AgentObservabilitySettingsOverride update(AgentObservabilitySettingsOverride override) {
        AgentObservabilitySettingsOverride next = override != null ? override : new AgentObservabilitySettingsOverride();
        runtimeOverride.set(next);
        return get();
    }

    public AgentObservabilitySettingsOverride clear() {
        runtimeOverride.set(new AgentObservabilitySettingsOverride());
        return get();
    }
}
