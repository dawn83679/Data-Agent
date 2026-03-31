package edu.zsc.ai.domain.service.agent.runtimecontext;

import edu.zsc.ai.domain.service.agent.prompt.AbstractPromptSectionHandler;

public abstract class AbstractRuntimeContextHandler
        extends AbstractPromptSectionHandler<RuntimeContextAssemblyContext, RuntimeContextSection>
        implements RuntimeContextHandler {
}
