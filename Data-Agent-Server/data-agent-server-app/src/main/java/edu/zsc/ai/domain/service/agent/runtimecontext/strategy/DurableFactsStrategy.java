package edu.zsc.ai.domain.service.agent.runtimecontext.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.PromptFormatSupport;
import edu.zsc.ai.domain.service.agent.runtimecontext.AbstractRuntimeContextHandler;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextAssemblyContext;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextSection;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;

@Component
public class DurableFactsStrategy extends AbstractRuntimeContextHandler {

    @Override
    protected RuntimeContextSection targetSection() {
        return RuntimeContextSection.DURABLE_FACTS;
    }

    @Override
    protected String buildContent(RuntimeContextAssemblyContext context) {
        List<MemoryRecallItem> durableFactMemories = context.getMemoryPromptContext().getMemories().stream()
                .filter(MemoryPromptProjectionSupport::isPromptInjectableNonPreferenceMemory)
                .filter(memory -> !MemoryPromptProjectionSupport.isScopeHintMemory(memory))
                .toList();
        return PromptFormatSupport.title(
                context.getLanguage(),
                "已知事实：",
                "Known durable facts:")
                + "\n"
                + MemoryPromptProjectionSupport.renderMemoryList(durableFactMemories);
    }
}
