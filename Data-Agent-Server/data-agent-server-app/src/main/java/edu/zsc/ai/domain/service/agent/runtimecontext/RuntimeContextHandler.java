package edu.zsc.ai.domain.service.agent.runtimecontext;

import edu.zsc.ai.domain.service.agent.prompt.PromptHandleRequest;
import edu.zsc.ai.domain.service.agent.prompt.PromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;

public interface RuntimeContextHandler extends PromptHandler<
        PromptHandleRequest<RuntimeContextAssemblyContext, RuntimeContextSection>,
        PromptSectionResult<RuntimeContextSection>> {
}
