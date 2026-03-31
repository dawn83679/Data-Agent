package edu.zsc.ai.domain.service.agent.runtimecontext.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.domain.service.agent.prompt.PromptFormatSupport;
import edu.zsc.ai.domain.service.agent.runtimecontext.AbstractRuntimeContextHandler;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextAssemblyContext;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextSection;

@Component
public class ResponsePreferencesStrategy extends AbstractRuntimeContextHandler {

    @Override
    protected RuntimeContextSection targetSection() {
        return RuntimeContextSection.RESPONSE_PREFERENCES;
    }

    @Override
    protected String buildContent(RuntimeContextAssemblyContext context) {
        List<String> lines = context.getMemoryPromptContext().getMemories().stream()
                .filter(MemoryPromptProjectionSupport::isPreferenceMemory)
                .map(memory -> memory.getContent())
                .toList();
        if (lines.isEmpty()) {
            return PromptFormatSupport.renderBlock(
                    context.getLanguage(),
                    "请默认遵循以下偏好：",
                    "Apply the following preferences by default:",
                    List.of(PromptConstant.NONE));
        }
        return PromptFormatSupport.renderBlock(
                context.getLanguage(),
                "请默认遵循以下偏好：",
                "Apply the following preferences by default:",
                lines);
    }
}
