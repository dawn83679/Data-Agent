package edu.zsc.ai.config.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.SkillEnum;

class AgentSkillConfigTest {

    private final AgentSkillConfig config = new AgentSkillConfig();

    @Test
    void mainAgentInNormalMode_hasOnlyChart() {
        assertEquals(List.of(SkillEnum.CHART),
                config.resolveAvailableSkills(AgentTypeEnum.MAIN, AgentModeEnum.AGENT));
    }

    @Test
    void mainAgentInPlanMode_hasNoSkills() {
        assertTrue(config.resolveAvailableSkills(AgentTypeEnum.MAIN, AgentModeEnum.PLAN).isEmpty());
    }

    @Test
    void planner_hasOnlySqlOptimization() {
        assertEquals(List.of(),
                config.resolveAvailableSkills(AgentTypeEnum.PLANNER, AgentModeEnum.AGENT));
    }

    @Test
    void memoryWriter_hasNoSkills() {
        assertEquals(List.of(),
                config.resolveAvailableSkills(AgentTypeEnum.MEMORY_WRITER, AgentModeEnum.AGENT));
    }

    @Test
    void supports_respectsAgentSkillVisibility() {
        assertFalse(config.supports(AgentTypeEnum.MAIN, AgentModeEnum.AGENT, "memory"));
        assertFalse(config.supports(AgentTypeEnum.MAIN, AgentModeEnum.AGENT, "file-export"));
        assertFalse(config.supports(AgentTypeEnum.PLANNER, AgentModeEnum.AGENT, "memory"));
        assertFalse(config.supports(AgentTypeEnum.PLANNER, AgentModeEnum.AGENT, "chart"));
        assertFalse(config.supports(AgentTypeEnum.EXPLORER, AgentModeEnum.AGENT, "chart"));
    }
}
