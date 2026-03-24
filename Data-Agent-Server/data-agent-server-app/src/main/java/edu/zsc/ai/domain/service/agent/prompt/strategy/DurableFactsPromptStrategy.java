package edu.zsc.ai.domain.service.agent.prompt.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.AbstractUserPromptHandler;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptAssemblyContext;
import edu.zsc.ai.domain.service.agent.prompt.UserPromptSection;
import edu.zsc.ai.domain.service.ai.recall.MemoryRecallItem;

@Component
public class DurableFactsPromptStrategy extends AbstractUserPromptHandler {

    @Override
    protected UserPromptSection targetSection() {
        return UserPromptSection.DURABLE_FACTS;
    }

    @Override
    protected String buildContent(UserPromptAssemblyContext context) {
        List<MemoryRecallItem> durableFactMemories = context.getMemoryPromptContext().getMemories().stream()
                .filter(MemoryPromptProjectionSupport::isPromptInjectableNonPreferenceMemory)
                .filter(memory -> !MemoryPromptProjectionSupport.isScopeHintMemory(memory))
                .toList();
        return UserPromptBlockSupport.title(
                context,
                "已知事实：",
                "Known durable facts:")
                + "\n"
                + MemoryPromptProjectionSupport.renderMemoryList(durableFactMemories);
    }
}
