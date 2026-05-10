package edu.zsc.ai.agent.tool.skill;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.SkillEnum;
import edu.zsc.ai.config.ai.AgentSkillConfig;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.context.AgentRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ActivateSkillTool {

    private final AgentSkillConfig agentSkillConfig;

    @Tool({
            "价值：加载特定能力的指导和模板，帮助后续工具使用。",
            "使用时机：当前任务明确需要可用技能列表中列出的某个技能。",
            "前置条件：skillName 必须匹配已列出的技能，且不应重复加载已加载的技能。",
            "结果：已加载技能的说明。",
            "边界：不要把技能激活本身当成面向用户的结果。"
    })
    public String activateSkill(
            @P("来自可用技能列表的技能名称。") String skillName) {
        SkillEnum skill = SkillEnum.fromName(skillName);
        if (skill == null) {
            return ToolMessageSupport.sentence(
                    "技能 `" + skillName + "` 不可用。",
                    "有效值：" + SkillEnum.validNames() + "。",
                    "不要假设该技能已加载。",
                    "只有任务仍需要时，才选择有效技能并重试。"
            );
        }

        AgentTypeEnum agentType = AgentTypeEnum.fromCode(AgentRequestContext.getAgentType());
        AgentModeEnum agentMode = AgentModeEnum.fromRequest(AgentRequestContext.getAgentMode());
        if (!agentSkillConfig.supports(agentType, agentMode, skillName)) {
            return ToolMessageSupport.sentence(
                    "技能 `" + skill.getSkillName() + "` 不适用于当前代理。",
                    "只能使用当前系统提示词可用技能列表中列出的技能。",
                    "不要假设该技能已加载。"
            );
        }

        log.info("Skill activated: {}, agentType={}, agentMode={}", skill.getSkillName(), agentType, agentMode);
        return PromptConfig.loadClassPathResource(skill.getResourcePath());
    }
}
