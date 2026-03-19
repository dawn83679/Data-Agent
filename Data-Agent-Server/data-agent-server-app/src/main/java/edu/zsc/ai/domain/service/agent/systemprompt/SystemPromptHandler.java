package edu.zsc.ai.domain.service.agent.systemprompt;

import edu.zsc.ai.domain.service.agent.prompt.PromptHandleRequest;
import edu.zsc.ai.domain.service.agent.prompt.PromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;

public interface SystemPromptHandler extends PromptHandler<
        PromptHandleRequest<SystemPromptAssemblyContext, SystemPromptSection>,
        PromptSectionResult<SystemPromptSection>> {
}
