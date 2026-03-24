package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;

@Component
public class ScopeHintsPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.SCOPE_HINTS;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        List<MemoryRecallItem> scopeHintMemories = context.getMemoryPromptContext().getMemories().stream()
                .filter(MemoryPromptProjectionSupport::isPromptInjectableNonPreferenceMemory)
                .filter(MemoryPromptProjectionSupport::isScopeHintMemory)
                .toList();
        return UserPromptBlockSupport.title(
                context,
                "请优先按以下范围理解和检索：",
                "Use the following scope guidance first:")
                + "\n"
                + MemoryPromptProjectionSupport.renderMemoryList(scopeHintMemories);
    }
}
