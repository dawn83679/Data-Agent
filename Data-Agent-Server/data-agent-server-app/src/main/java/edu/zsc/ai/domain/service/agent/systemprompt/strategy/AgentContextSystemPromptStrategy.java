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
        return "agent 类型：" + PromptTextUtil.escape(context.getAgentType().getCode()) + "\n"
                + "语言：" + PromptTextUtil.escape(context.getLanguage()) + "\n"
                + "模型名称：" + PromptTextUtil.escape(context.getModelName());
    }
}
