package edu.zsc.ai.agent.tool.skill;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.common.enums.ai.SkillEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
public class ActivateSkillTool {

    @Tool({
            "Calling this before first use of a capability greatly improves output quality — you get expert ",
            "rules and templates (e.g. for charts). Skip if already loaded this session. ",
            "Loads expert rules and templates for a task type (e.g. chart).",
            "",
            "When to Use: before first renderChart in the conversation to load ECharts rules.",
            "When NOT to Use: when the skill was already loaded in this session.",
            "Relation: call before first use of the capability (e.g. activateSkill('chart') before renderChart). skillName must be one of: chart."
    })
    public String activateSkill(
            @P("Skill to load. MUST be one of: chart") String skillName) {
        SkillEnum skill = SkillEnum.fromName(skillName);
        if (skill == null) {
            return "Unknown skill: " + skillName
                    + ". Valid values: " + SkillEnum.validNames();
        }
        log.info("Skill activated: {}", skill.getSkillName());
        return PromptConfig.loadClassPathResource(skill.getResourcePath());
    }
}
