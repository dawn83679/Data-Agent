package edu.zsc.ai.agent.subagent.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaSummary {
    private String summaryText;
    private String rawResponse;
    private List<ExploreObject> objects;
}
