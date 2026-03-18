package edu.zsc.ai.observability.config;

public interface AgentObservabilityConfigProvider {

    AgentObservabilitySettings current();

    AgentObservabilitySnapshot snapshot();
}
