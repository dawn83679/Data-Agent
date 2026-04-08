package edu.zsc.ai.domain.service.agent.systemprompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.common.constant.SkillPromptTagConstant;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.common.enums.ai.SkillEnum;
import edu.zsc.ai.domain.service.agent.systemprompt.strategy.AgentContextSystemPromptStrategy;
import edu.zsc.ai.domain.service.agent.systemprompt.strategy.AgentModeSystemPromptStrategy;
import edu.zsc.ai.domain.service.agent.systemprompt.strategy.SkillAvailableSystemPromptStrategy;
import edu.zsc.ai.domain.service.agent.systemprompt.strategy.ToolUsageRulesSystemPromptStrategy;

class SystemPromptManagerTest {

    private final SystemPromptManager manager = new SystemPromptManager(new SystemPromptHandlerChain(List.of(
            new AgentContextSystemPromptStrategy(),
            new AgentModeSystemPromptStrategy(),
            new SkillAvailableSystemPromptStrategy(),
            new ToolUsageRulesSystemPromptStrategy()
    )));

    @Test
    void mainAgentPrompt_hasNoMemoryToolHintsWithoutMemorySkillBlock() {
        String prompt = manager.render(SystemPromptAssemblyContext.builder()
                .promptEnum(PromptEnum.ZH)
                .agentType(AgentTypeEnum.MAIN)
                .agentMode(AgentModeEnum.AGENT)
                .language("zh")
                .modelName("qwen3-max-2026-01-23")
                .availableSkills(List.of(SkillEnum.CHART))
                .build()).renderedPrompt();

<<<<<<< HEAD
        assertFalse(prompt.contains("<available_connections"));
        assertFalse(prompt.contains("<name>test1</name>"));
        assertFalse(prompt.contains("<name>test2</name>"));
        assertFalse(prompt.contains("<name>test3</name>"));
        assertTrue(prompt.contains("可用连接"));
        assertTrue(prompt.contains("getAvailableConnections"));
=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
        assertFalse(prompt.contains(SkillPromptTagConstant.open("memory")));
        assertFalse(prompt.contains(SkillPromptTagConstant.close("memory")));
        assertTrue(prompt.contains(SkillPromptTagConstant.open(SkillEnum.CHART.getSkillName())));
        assertFalse(prompt.contains("readMemory"));
        assertFalse(prompt.contains("updateMemory"));
        assertFalse(prompt.contains("memory agent"),
                "System prompt should not mention the memory agent");
    }

    @Test
    void plannerPrompt_withoutSkills_hasNoSkillBlocks() {
        String prompt = manager.render(SystemPromptAssemblyContext.builder()
                .promptEnum(PromptEnum.PLANNER)
                .agentType(AgentTypeEnum.PLANNER)
                .agentMode(AgentModeEnum.AGENT)
                .language("zh")
                .modelName("qwen3-max-2026-01-23")
                .availableSkills(List.of())
                .build()).renderedPrompt();

<<<<<<< HEAD
        assertFalse(prompt.contains("<available_connections"));
=======
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
        assertFalse(prompt.contains(SkillPromptTagConstant.open(SkillEnum.CHART.getSkillName())));
        assertFalse(prompt.contains(SkillPromptTagConstant.open("memory")));
        assertFalse(prompt.contains(SkillPromptTagConstant.close("memory")));
    }
}
