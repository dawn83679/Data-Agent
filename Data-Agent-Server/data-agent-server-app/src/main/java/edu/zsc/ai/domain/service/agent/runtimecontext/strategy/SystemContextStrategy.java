package edu.zsc.ai.domain.service.agent.runtimecontext.strategy;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import edu.zsc.ai.domain.service.agent.prompt.PromptFormatSupport;
import edu.zsc.ai.domain.service.agent.runtimecontext.AbstractRuntimeContextHandler;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextAssemblyContext;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextSection;

@Component
public class SystemContextStrategy extends AbstractRuntimeContextHandler {

    @Override
    protected RuntimeContextSection targetSection() {
        return RuntimeContextSection.SYSTEM_CONTEXT;
    }

    @Override
    protected String buildContent(RuntimeContextAssemblyContext context) {
        return PromptFormatSupport.renderBlock(
                context.getLanguage(),
                "当前运行时环境：",
                "Current runtime environment:",
                List.of(
                        "today: " + (context.getCurrentDate() == null ? "" : context.getCurrentDate().toString()),
                        "timezone: " + StringUtils.defaultString(context.getTimezone())
                ));
    }
}
