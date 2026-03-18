package edu.zsc.ai.domain.service.ai.impl;

import edu.zsc.ai.api.model.request.ai.AgentObservabilityUpdateRequest;
import edu.zsc.ai.domain.service.ai.AgentObservabilityAdminService;
import edu.zsc.ai.observability.config.AgentObservabilityConfigProvider;
import edu.zsc.ai.observability.config.AgentObservabilityRuntimeState;
import edu.zsc.ai.observability.config.AgentObservabilitySettingsOverride;
import edu.zsc.ai.observability.config.AgentObservabilitySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentObservabilityAdminServiceImpl implements AgentObservabilityAdminService {

    private final AgentObservabilityConfigProvider configProvider;
    private final AgentObservabilityRuntimeState runtimeState;

    @Override
    public AgentObservabilitySnapshot getSnapshot() {
        return configProvider.snapshot();
    }

    @Override
    public AgentObservabilitySnapshot updateRuntimeOverride(AgentObservabilityUpdateRequest request) {
        AgentObservabilitySettingsOverride override = AgentObservabilitySettingsOverride.builder()
                .enabled(request.getEnabled())
                .runtimeLogEnabled(request.getRuntimeLogEnabled())
                .consoleLogEnabled(request.getConsoleLogEnabled())
                .sseEventLogEnabled(request.getSseEventLogEnabled())
                .modelEventLogEnabled(request.getModelEventLogEnabled())
                .toolEventLogEnabled(request.getToolEventLogEnabled())
                .includePrompt(request.getIncludePrompt())
                .includeResponse(request.getIncludeResponse())
                .includeTokenStream(request.getIncludeTokenStream())
                .runtimeLogDir(request.getRuntimeLogDir())
                .build();
        runtimeState.update(override);
        return configProvider.snapshot();
    }

    @Override
    public AgentObservabilitySnapshot clearRuntimeOverride() {
        runtimeState.clear();
        return configProvider.snapshot();
    }
}
