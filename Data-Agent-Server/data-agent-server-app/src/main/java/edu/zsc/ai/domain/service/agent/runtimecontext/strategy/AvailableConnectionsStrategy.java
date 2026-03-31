package edu.zsc.ai.domain.service.agent.runtimecontext.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import edu.zsc.ai.common.constant.PromptConstant;
import edu.zsc.ai.domain.service.agent.prompt.PromptFormatSupport;
import edu.zsc.ai.domain.service.agent.runtimecontext.AbstractRuntimeContextHandler;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextAssemblyContext;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextSection;

@Component
public class AvailableConnectionsStrategy extends AbstractRuntimeContextHandler {

    @Override
    protected RuntimeContextSection targetSection() {
        return RuntimeContextSection.AVAILABLE_CONNECTIONS;
    }

    @Override
    protected String buildContent(RuntimeContextAssemblyContext context) {
        List<ConnectionSummary> connections = context.getAvailableConnections();
        if (connections == null || connections.isEmpty()) {
            return PromptFormatSupport.renderBlock(
                    context.getLanguage(),
                    "可用的数据库连接：",
                    "Available database connections:",
                    List.of(PromptConstant.NONE));
        }
        List<String> lines = connections.stream()
                .map(conn -> "id=" + conn.id() + ", name=" + conn.name() + ", type=" + conn.dbType())
                .toList();
        return PromptFormatSupport.renderBlock(
                context.getLanguage(),
                "可用的数据库连接：",
                "Available database connections:",
                lines);
    }
}
