package edu.zsc.ai.config.ai;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.SkillEnum;

@Component
public class AgentSkillConfig {

    private final Map<AgentTypeEnum, List<SkillEnum>> defaultSkillsByAgent = new EnumMap<>(AgentTypeEnum.class);

    public AgentSkillConfig() {
        defaultSkillsByAgent.put(AgentTypeEnum.MAIN, List.of(SkillEnum.CHART, SkillEnum.MEMORY));
        defaultSkillsByAgent.put(AgentTypeEnum.PLANNER, List.of(SkillEnum.SQL_OPTIMIZATION));
        defaultSkillsByAgent.put(AgentTypeEnum.EXPLORER, List.of());
    }

    public List<SkillEnum> resolveAvailableSkills(AgentTypeEnum agentType, AgentModeEnum mode) {
        if (agentType == AgentTypeEnum.MAIN && mode == AgentModeEnum.PLAN) {
            return List.of();
        }
        return defaultSkillsByAgent.getOrDefault(agentType, List.of());
    }

    public boolean supports(AgentTypeEnum agentType, AgentModeEnum mode, String skillName) {
        SkillEnum skill = SkillEnum.fromName(skillName);
        if (skill == null) {
            return false;
        }
        return resolveAvailableSkills(agentType, mode).contains(skill);
    }
}
