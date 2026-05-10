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
        return "当前任务的范围提示：\n\n"
                + "把下面内容作为本任务优先起点范围。\n"
                + "如果提示已经足够具体，在考虑更大范围发现前先留在该范围内验证。\n\n"
                + MemoryPromptProjectionSupport.renderScopeHintList(scopeHintMemories);
    }
}
