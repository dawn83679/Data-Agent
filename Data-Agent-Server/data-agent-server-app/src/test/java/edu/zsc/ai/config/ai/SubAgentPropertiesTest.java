package edu.zsc.ai.config.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SubAgentProperties — verifies default values and mutability.
 */
class SubAgentPropertiesTest {

    @Test
    void defaultValues_explorerTimeout() {
        SubAgentProperties props = new SubAgentProperties();
        assertEquals(180, props.getExplorer().getTimeoutSeconds());
    }

    @Test
    void defaultValues_explorerDispatch() {
        SubAgentProperties props = new SubAgentProperties();
        assertEquals(3, props.getExplorer().getDispatch().getMaxConcurrency());
        assertEquals(9, props.getExplorer().getDispatch().getQueueCapacity());
    }

    @Test
    void defaultValues_plannerTimeout() {
        SubAgentProperties props = new SubAgentProperties();
        assertEquals(180, props.getPlanner().getTimeoutSeconds());
    }

    @Test
    void defaultValues_maxExplorerLoop() {
        SubAgentProperties props = new SubAgentProperties();
        assertEquals(3, props.getMaxExplorerLoop());
    }

    @Test
    void canOverridePlannerTimeout() {
        SubAgentProperties props = new SubAgentProperties();
        props.getPlanner().setTimeoutSeconds(300);
        assertEquals(300, props.getPlanner().getTimeoutSeconds());
    }

    @Test
    void canOverrideMaxExplorerLoop() {
        SubAgentProperties props = new SubAgentProperties();
        props.setMaxExplorerLoop(5);
        assertEquals(5, props.getMaxExplorerLoop());
    }

    @Test
    void agentConfig_defaultConstructor() {
        SubAgentProperties.AgentConfig config = new SubAgentProperties.AgentConfig();
        assertEquals(180, config.getTimeoutSeconds());
    }

    @Test
    void agentConfig_parameterizedConstructor() {
        SubAgentProperties.AgentConfig config = new SubAgentProperties.AgentConfig(60);
        assertEquals(60, config.getTimeoutSeconds());
    }
}
