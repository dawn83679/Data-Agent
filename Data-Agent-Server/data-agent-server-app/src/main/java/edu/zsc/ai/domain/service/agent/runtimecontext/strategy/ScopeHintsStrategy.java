package edu.zsc.ai.domain.service.agent.runtimecontext.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.runtimecontext.AbstractRuntimeContextHandler;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextAssemblyContext;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextSection;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;

@Component
public class ScopeHintsStrategy extends AbstractRuntimeContextHandler {

    @Override
    protected RuntimeContextSection targetSection() {
        return RuntimeContextSection.SCOPE_HINTS;
    }

    @Override
    protected String buildContent(RuntimeContextAssemblyContext context) {
        List<MemoryRecallItem> scopeHintMemories = context.getMemoryPromptContext().getMemories().stream()
                .filter(MemoryPromptProjectionSupport::isPromptInjectableNonPreferenceMemory)
                .filter(MemoryPromptProjectionSupport::isScopeHintMemory)
                .toList();
        return "Helpful scope hints for this task:\n\n"
                + "Treat the following as the preferred starting scope for this task.\n"
                + "If the current hints are already specific enough, stay within that scope before considering broader discovery.\n\n"
                + MemoryPromptProjectionSupport.renderScopeHintList(scopeHintMemories);
    }
}
