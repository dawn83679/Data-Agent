package edu.zsc.ai.domain.service.agent.systemprompt.strategy;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.PromptTextUtil;
import edu.zsc.ai.domain.service.agent.systemprompt.AbstractSystemPromptHandler;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.systemprompt.SystemPromptSection;

@Component
public class AgentContextSystemPromptStrategy extends AbstractSystemPromptHandler {

    @Override
    protected SystemPromptSection targetSection() {
        return SystemPromptSection.AGENT_CONTEXT;
    }

    @Override
    protected String buildContent(SystemPromptAssemblyContext context) {
        return "agent_type: " + PromptTextUtil.escape(context.getAgentType().getCode()) + "\n"
                + "language: " + PromptTextUtil.escape(context.getLanguage()) + "\n"
                + "model_name: " + PromptTextUtil.escape(context.getModelName());
    }
}
