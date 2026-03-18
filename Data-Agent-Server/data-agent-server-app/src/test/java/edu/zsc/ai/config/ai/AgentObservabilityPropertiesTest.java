package edu.zsc.ai.config.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentObservabilityPropertiesTest {

    @Test
    void defaultValues_matchExpectedRuntimeLoggingDefaults() {
        AgentObservabilityProperties properties = new AgentObservabilityProperties();

        assertFalse(properties.isEnabled());
        assertFalse(properties.isRuntimeLogEnabled());
        assertTrue(properties.isConsoleLogEnabled());
        assertTrue(properties.isSseEventLogEnabled());
        assertTrue(properties.isModelEventLogEnabled());
        assertTrue(properties.isToolEventLogEnabled());
        assertFalse(properties.isIncludePrompt());
        assertTrue(properties.isIncludeResponse());
        assertTrue(properties.isIncludeTokenStream());
        assertEquals("~/.data-agent/logs/agent/runtime", properties.getRuntimeLogDir());
    }
}
