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
    void mainAgentPrompt_mentionsMemoryToolsWithoutMemorySkillBlock() {
        String prompt = manager.render(SystemPromptAssemblyContext.builder()
                .promptEnum(PromptEnum.ZH)
                .agentType(AgentTypeEnum.MAIN)
                .agentMode(AgentModeEnum.AGENT)
                .language("zh")
                .modelName("qwen3-max")
                .availableSkills(List.of(SkillEnum.CHART))
                .build()).renderedPrompt();

        assertFalse(prompt.contains(SkillPromptTagConstant.open("memory")));
        assertFalse(prompt.contains(SkillPromptTagConstant.close("memory")));
        assertTrue(prompt.contains(SkillPromptTagConstant.open(SkillEnum.CHART.getSkillName())));
        assertTrue(prompt.contains("writeMemory"));
        assertTrue(prompt.contains("examples of memory-worthy signals"));
    }

    @Test
    void plannerPrompt_withoutSkills_hasNoSkillBlocks() {
        String prompt = manager.render(SystemPromptAssemblyContext.builder()
                .promptEnum(PromptEnum.PLANNER)
                .agentType(AgentTypeEnum.PLANNER)
                .agentMode(AgentModeEnum.AGENT)
                .language("zh")
                .modelName("qwen3-max")
                .availableSkills(List.of())
                .build()).renderedPrompt();

        assertFalse(prompt.contains(SkillPromptTagConstant.open(SkillEnum.CHART.getSkillName())));
        assertFalse(prompt.contains(SkillPromptTagConstant.open("memory")));
        assertFalse(prompt.contains(SkillPromptTagConstant.close("memory")));
    }
}
