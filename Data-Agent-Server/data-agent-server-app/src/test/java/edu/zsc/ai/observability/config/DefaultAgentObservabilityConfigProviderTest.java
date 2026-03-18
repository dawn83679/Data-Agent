package edu.zsc.ai.observability.config;

import edu.zsc.ai.config.ai.AgentObservabilityProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAgentObservabilityConfigProviderTest {

    @Test
    void current_mergesRuntimeOverrideOnTopOfConfiguredSettings() {
        AgentObservabilityProperties properties = new AgentObservabilityProperties();
        properties.setEnabled(false);
        properties.setRuntimeLogEnabled(false);
        properties.setConsoleLogEnabled(true);
        properties.setRuntimeLogDir("~/.data-agent/logs/agent/runtime");

        AgentObservabilityRuntimeState runtimeState = new AgentObservabilityRuntimeState();
        runtimeState.update(AgentObservabilitySettingsOverride.builder()
                .enabled(true)
                .runtimeLogEnabled(true)
                .runtimeLogDir("~/tmp/runtime")
                .build());

        DefaultAgentObservabilityConfigProvider provider =
                new DefaultAgentObservabilityConfigProvider(properties, runtimeState);

        AgentObservabilitySettings effective = provider.current();

        assertTrue(effective.isEnabled());
        assertTrue(effective.isRuntimeLogEnabled());
        assertTrue(effective.isConsoleLogEnabled());
        assertEquals("~/tmp/runtime", effective.getRuntimeLogDir());
    }

    @Test
    void clearRuntimeOverride_restoresConfiguredSettings() {
        AgentObservabilityProperties properties = new AgentObservabilityProperties();
        properties.setEnabled(false);
        properties.setRuntimeLogEnabled(false);

        AgentObservabilityRuntimeState runtimeState = new AgentObservabilityRuntimeState();
        runtimeState.update(AgentObservabilitySettingsOverride.builder()
                .enabled(true)
                .runtimeLogEnabled(true)
                .build());
        runtimeState.clear();

        DefaultAgentObservabilityConfigProvider provider =
                new DefaultAgentObservabilityConfigProvider(properties, runtimeState);

        AgentObservabilitySettings effective = provider.current();

        assertFalse(effective.isEnabled());
        assertFalse(effective.isRuntimeLogEnabled());
    }
}
