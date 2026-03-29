package edu.zsc.ai.agent.subagent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubAgentTimeoutPolicyTest {

    @Test
    void usesDefaultWhenRequestIsMissing() {
        assertEquals(180L, SubAgentTimeoutPolicy.normalizeTimeoutSeconds(null, 180L));
    }

    @Test
    void raisesRequestedTimeoutBelowMinimum() {
        assertEquals(180L, SubAgentTimeoutPolicy.normalizeTimeoutSeconds(30L, 180L));
        assertEquals(180L, SubAgentTimeoutPolicy.normalizeTimeoutSeconds(75L, 180L));
    }

    @Test
    void keepsRequestedTimeoutWhenItIsAboveMinimum() {
        assertEquals(240L, SubAgentTimeoutPolicy.normalizeTimeoutSeconds(240L, 180L));
    }
}
