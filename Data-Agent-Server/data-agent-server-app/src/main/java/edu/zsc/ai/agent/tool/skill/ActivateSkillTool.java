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
            "Value: loads capability-specific guidance and templates that improve later tool use.",
            "Use When: a skill listed in <skill_available> is clearly needed for the current task.",
            "Preconditions: skillName must match a listed skill and should not already be loaded.",
            "Result: loaded skill instructions.",
            "Boundary: do not present skill activation itself as the user-facing result."
    })
    public String activateSkill(
            @P("Skill name from <skill_available>.") String skillName) {
        SkillEnum skill = SkillEnum.fromName(skillName);
        if (skill == null) {
            return ToolMessageSupport.sentence(
                    "Skill '" + skillName + "' is not available.",
                    "Valid values: " + SkillEnum.validNames() + ".",
                    "Do not assume the skill is loaded.",
                    "Choose a valid skill and retry only if the task still needs it."
            );
        }

        AgentTypeEnum agentType = AgentTypeEnum.fromCode(AgentRequestContext.getAgentType());
        AgentModeEnum agentMode = AgentModeEnum.fromRequest(AgentRequestContext.getAgentMode());
        if (!agentSkillConfig.supports(agentType, agentMode, skillName)) {
            return ToolMessageSupport.sentence(
                    "Skill '" + skill.getSkillName() + "' is not available for the current agent.",
                    "Only use skills listed in <skill_available> of the current system prompt.",
                    "Do not assume the skill is loaded."
            );
        }

        log.info("Skill activated: {}, agentType={}, agentMode={}", skill.getSkillName(), agentType, agentMode);
        return PromptConfig.loadClassPathResource(skill.getResourcePath());
    }
}
