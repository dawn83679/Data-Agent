package edu.zsc.ai.agent.tool.skill;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.common.enums.ai.SkillEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
public class ActivateSkillTool {

    @Tool({
            "Value: loads capability-specific guidance and templates that improve later tool use.",
            "Use When: call before the first renderChart in a session, or before complex SQL optimization work that needs specialized guidance.",
            "After Success: immediately apply the loaded rules in later tool calls. Do not present skill activation itself as the user-facing result.",
            "After Failure: choose a valid skill name or continue without the skill only if the quality tradeoff is acceptable.",
            "Relation: use chart before renderChart. Use sql-optimization before planner or SQL optimization work when joins, subqueries, or performance tuning are central.",
            "skillName must be one of: chart, sql-optimization. Skip if the skill is already loaded in this session."
    })
    public String activateSkill(
            @P("Skill to load. MUST be one of: chart, sql-optimization") String skillName) {
        SkillEnum skill = SkillEnum.fromName(skillName);
        if (skill == null) {
            return ToolMessageSupport.sentence(
                    "Skill '" + skillName + "' is not available.",
                    "Valid values: " + SkillEnum.validNames() + ".",
                    "Do not assume the skill is loaded.",
                    "Choose a valid skill and retry only if the task still needs it."
            );
        }
        log.info("Skill activated: {}", skill.getSkillName());
        return PromptConfig.loadClassPathResource(skill.getResourcePath());
    }
}
