package edu.zsc.ai.agent.tool.orchestrator;

import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import edu.zsc.ai.agent.subagent.contract.ExploreObjectScoreSupport;
import edu.zsc.ai.agent.subagent.contract.ExplorerResultEnvelope;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskResult;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskStatus;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.util.JsonUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Accepts either a plain {@link SchemaSummary} payload or the explorer task envelope payload.
 */
@Component
public class DefaultSchemaSummaryResolver implements SchemaSummaryResolver {

    @Override
    public SchemaSummary resolve(String schemaSummaryJson) {
        try {
            ExplorerResultEnvelope envelope = JsonUtil.json2Object(schemaSummaryJson, ExplorerResultEnvelope.class);
            if (envelope != null && CollectionUtils.isNotEmpty(envelope.getTaskResults())) {
                return mergeEnvelope(envelope);
            }
        } catch (Exception ignored) {
            // Fall through to plain SchemaSummary parsing.
        }
        SchemaSummary summary = JsonUtil.json2Object(schemaSummaryJson, SchemaSummary.class);
        ExploreObjectScoreSupport.normalizeAndSort(summary.getObjects());
        return summary;
    }

    private SchemaSummary mergeEnvelope(ExplorerResultEnvelope envelope) {
        List<ExploreObject> mergedObjects = new ArrayList<>();
        StringBuilder summaryText = new StringBuilder();
        StringBuilder rawResponse = new StringBuilder();
        for (ExplorerTaskResult taskResult : envelope.getTaskResults()) {
            if (taskResult.getStatus() == ExplorerTaskStatus.ERROR) {
                continue;
            }
            if (CollectionUtils.isNotEmpty(taskResult.getObjects())) {
                mergedObjects.addAll(taskResult.getObjects());
            }
            appendIfPresent(summaryText, taskResult.getSummaryText(), "\n");
            appendIfPresent(rawResponse, taskResult.getRawResponse(), "\n\n");
        }
        return SchemaSummary.builder()
                .summaryText(summaryText.toString())
                .rawResponse(rawResponse.toString())
                .objects(ExploreObjectScoreSupport.normalizeAndSort(mergedObjects))
                .build();
    }

    private void appendIfPresent(StringBuilder buffer, String value, String delimiter) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (buffer.length() > 0) {
            buffer.append(delimiter);
        }
        buffer.append(value);
    }
}
