package edu.zsc.ai.agent.tool.skill;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import edu.zsc.ai.config.ai.AgentSkillConfig;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;

class ActivateSkillToolTest {

    private final ActivateSkillTool tool = new ActivateSkillTool(new AgentSkillConfig());

    @AfterEach
    void tearDown() {
        AgentRequestContext.clear();
    }

    @Test
    void mainAgentCanLoadMemorySkill() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("main")
                .agentMode("agent")
                .build());

        String result = tool.activateSkill("memory");

        assertTrue(result.contains("writeMemory"));
        assertTrue(result.contains("readMemory"));
        assertTrue(result.contains("PREFERENCE"));
        assertTrue(result.contains("Example A - Stable user preference"));
        assertTrue(result.contains("workspaceLevel=`CATALOG`"));
        assertTrue(result.contains("Monthly revenue in analytics is computed"));
    }

    @Test
    void plannerCannotLoadMemorySkill() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("planner")
                .agentMode("agent")
                .build());

        String result = tool.activateSkill("memory");

        assertTrue(result.contains("not available for the current agent"));
    }
}
