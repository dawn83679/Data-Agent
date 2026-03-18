package edu.zsc.ai.agent.tool.orchestrator;

import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import edu.zsc.ai.agent.subagent.contract.ExplorerResultEnvelope;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskResult;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskStatus;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSchemaSummaryResolverTest {

    private final DefaultSchemaSummaryResolver resolver = new DefaultSchemaSummaryResolver();

    @Test
    void resolvesPlainSchemaSummary() {
        SchemaSummary summary = SchemaSummary.builder()
                .summaryText("users table")
                .objects(List.of(ExploreObject.builder().objectName("users").build()))
                .build();

        SchemaSummary resolved = resolver.resolve(JsonUtil.object2json(summary));

        assertEquals("users table", resolved.getSummaryText());
        assertEquals(1, resolved.getObjects().size());
    }

    @Test
    void mergesExplorerEnvelopeAndSkipsFailedTasks() {
        ExplorerResultEnvelope envelope = ExplorerResultEnvelope.builder()
                .taskResults(List.of(
                        ExplorerTaskResult.builder()
                                .taskId("ok")
                                .status(ExplorerTaskStatus.SUCCESS)
                                .summaryText("users")
                                .rawResponse("users raw")
                                .objects(List.of(ExploreObject.builder().objectName("users").relevanceScore(40).build()))
                                .build(),
                        ExplorerTaskResult.builder()
                                .taskId("err")
                                .status(ExplorerTaskStatus.ERROR)
                                .summaryText("should skip")
                                .rawResponse("skip raw")
                                .objects(List.of(ExploreObject.builder().objectName("orders").build()))
                                .build()
                ))
                .build();

        SchemaSummary resolved = resolver.resolve(JsonUtil.object2json(envelope));

        assertEquals("users", resolved.getSummaryText());
        assertEquals("users raw", resolved.getRawResponse());
        assertEquals(List.of("users"), resolved.getObjects().stream().map(ExploreObject::getObjectName).toList());
        assertEquals(40, resolved.getObjects().get(0).getRelevanceScore());
    }

    @Test
    void mergesExplorerEnvelope_sortsObjectsByRelevanceScore() {
        ExplorerResultEnvelope envelope = ExplorerResultEnvelope.builder()
                .taskResults(List.of(
                        ExplorerTaskResult.builder()
                                .taskId("ok")
                                .status(ExplorerTaskStatus.SUCCESS)
                                .objects(List.of(
                                        ExploreObject.builder().objectName("audit_log").relevanceScore(20).build(),
                                        ExploreObject.builder().objectName("orders").relevanceScore(90).build()
                                ))
                                .build()
                ))
                .build();

        SchemaSummary resolved = resolver.resolve(JsonUtil.object2json(envelope));

        assertEquals(List.of("orders", "audit_log"),
                resolved.getObjects().stream().map(ExploreObject::getObjectName).toList());
    }
}
