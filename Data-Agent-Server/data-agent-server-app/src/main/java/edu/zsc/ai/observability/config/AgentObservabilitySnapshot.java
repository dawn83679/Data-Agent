package edu.zsc.ai.observability.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentObservabilitySnapshot {

    private AgentObservabilitySettings configured;
    private AgentObservabilitySettingsOverride runtimeOverride;
    private AgentObservabilitySettings effective;
}
