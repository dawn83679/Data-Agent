package edu.zsc.ai.domain.service.agent.systemprompt;

import java.util.List;

import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.common.enums.ai.SkillEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SystemPromptAssemblyContext {

    private PromptEnum promptEnum;

    private AgentTypeEnum agentType;

    private AgentModeEnum agentMode;

    private String modelName;

    private String language;

    @Builder.Default
    private List<SkillEnum> availableSkills = List.of();
}
