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
    void mainAgentCannotLoadUnknownMemorySkill() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("main")
                .agentMode("agent")
                .build());

        String result = tool.activateSkill("memory");

        assertTrue(result.contains("Skill 'memory' is not available."));
    }

    @Test
    void plannerCannotLoadUnknownMemorySkill() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("planner")
                .agentMode("agent")
                .build());

        String result = tool.activateSkill("memory");

        assertTrue(result.contains("Skill 'memory' is not available."));
    }

    @Test
    void mainAgentRejectsRemovedFileExportSkill() {
        AgentRequestContext.set(AgentRequestContextInfo.builder()
                .agentType("main")
                .agentMode("agent")
                .build());

        String result = tool.activateSkill("file-export");

        assertTrue(result.contains("Skill 'file-export' is not available."));
    }
}
