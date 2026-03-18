package edu.zsc.ai.agent.subagent.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Final result for one explorer task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorerTaskResult {

    private String taskId;

    private ExplorerTaskStatus status;

    private String summaryText;

    private List<ExploreObject> objects;

    private String rawResponse;

    private String errorMessage;
}
