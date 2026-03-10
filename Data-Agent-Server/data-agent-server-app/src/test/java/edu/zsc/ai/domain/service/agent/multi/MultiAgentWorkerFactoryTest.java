package edu.zsc.ai.domain.service.agent.multi;

import edu.zsc.ai.agent.MultiAgentWorker;
import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MultiAgentWorkerFactoryTest {

    @Test
    void shouldResolveWorkerFromExplicitRoleMap() {
        MultiAgentWorker schemaWorker = mock(MultiAgentWorker.class);
        MultiAgentWorker plannerWorker = mock(MultiAgentWorker.class);

        MultiAgentWorkerFactory factory = new MultiAgentWorkerFactory(
                Map.of("qwen3-max", schemaWorker),
                Map.of("qwen3-max", plannerWorker),
                Map.of(),
                Map.of());

        assertSame(schemaWorker, factory.getWorker("qwen3-max", "zh", AgentRoleEnum.SCHEMA_ANALYST));
        assertSame(plannerWorker, factory.getWorker("qwen3-max", "zh", AgentRoleEnum.SQL_PLANNER));
    }

    @Test
    void shouldShowAvailableModelsWhenRequestedModelMissing() {
        MultiAgentWorkerFactory factory = new MultiAgentWorkerFactory(
                Map.of("qwen-plus", mock(MultiAgentWorker.class)),
                Map.of(),
                Map.of(),
                Map.of());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> factory.getWorker("qwen3-max", "zh", AgentRoleEnum.SCHEMA_ANALYST));

        assertTrue(error.getMessage().contains("qwen-plus"));
    }
}
