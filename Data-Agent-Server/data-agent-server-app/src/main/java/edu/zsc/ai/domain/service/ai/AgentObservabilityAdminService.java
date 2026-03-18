package edu.zsc.ai.domain.service.ai;

import edu.zsc.ai.api.model.request.ai.AgentObservabilityUpdateRequest;
import edu.zsc.ai.observability.config.AgentObservabilitySnapshot;

public interface AgentObservabilityAdminService {

    AgentObservabilitySnapshot getSnapshot();

    AgentObservabilitySnapshot updateRuntimeOverride(AgentObservabilityUpdateRequest request);

    AgentObservabilitySnapshot clearRuntimeOverride();
}
