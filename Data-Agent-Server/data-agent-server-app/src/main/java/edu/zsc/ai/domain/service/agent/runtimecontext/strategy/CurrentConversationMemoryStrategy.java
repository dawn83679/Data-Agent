package edu.zsc.ai.domain.service.agent.runtimecontext.strategy;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.domain.service.agent.prompt.PromptFormatSupport;
import edu.zsc.ai.domain.service.agent.runtimecontext.AbstractRuntimeContextHandler;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextAssemblyContext;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextSection;

@Component
public class CurrentConversationMemoryStrategy extends AbstractRuntimeContextHandler {

    @Override
    protected RuntimeContextSection targetSection() {
        return RuntimeContextSection.CURRENT_CONVERSATION_MEMORY;
    }

    @Override
    protected String buildContent(RuntimeContextAssemblyContext context) {
        String markdown = context.getMemoryPromptContext().getCurrentConversationMemory();
        if (StringUtils.isBlank(markdown)) {
            return PromptFormatSupport.renderBlock(
                    context.getLanguage(),
                    "当前会话工作记忆：",
                    "Current conversation working memory:",
                    List.of(PromptConstant.NONE));
        }
        return PromptFormatSupport.title(
                context.getLanguage(),
                "当前会话工作记忆：",
                "Current conversation working memory:")
                + "\n"
                + markdown.strip();
    }
}
