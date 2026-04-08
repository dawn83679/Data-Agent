package edu.zsc.ai.domain.service.agent.runtimecontext.strategy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.zsc.ai.domain.service.agent.prompt.PromptHandleRequest;
import edu.zsc.ai.domain.service.agent.prompt.PromptSectionResult;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextAssemblyContext;
import edu.zsc.ai.domain.service.agent.runtimecontext.RuntimeContextSection;

class AvailableConnectionsStrategyTest {

    private final AvailableConnectionsStrategy strategy = new AvailableConnectionsStrategy();

    @Test
    void handle_rendersKnownConnections() {
        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("en")
                .availableConnections(List.of(
                        new ConnectionSummary(7L, "analytics-prod", "POSTGRESQL"),
                        new ConnectionSummary(9L, "crm-main", "MYSQL")))
                .build();

        PromptSectionResult<RuntimeContextSection> result = strategy.handle(
                new PromptHandleRequest<>(context, RuntimeContextSection.AVAILABLE_CONNECTIONS));

        assertTrue(result.content().contains("Available database connections:"));
        assertTrue(result.content().contains("id=7, name=analytics-prod, type=POSTGRESQL"));
        assertTrue(result.content().contains("id=9, name=crm-main, type=MYSQL"));
    }

    @Test
    void handle_rendersNoneFallbackWhenNoConnectionsAvailable() {
        RuntimeContextAssemblyContext context = RuntimeContextAssemblyContext.builder()
                .language("zh")
                .availableConnections(List.of())
                .build();

        PromptSectionResult<RuntimeContextSection> result = strategy.handle(
                new PromptHandleRequest<>(context, RuntimeContextSection.AVAILABLE_CONNECTIONS));

        assertTrue(result.content().contains("可用的数据库连接："));
        assertTrue(result.content().contains("- none"));
    }
}
