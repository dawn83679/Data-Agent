package edu.zsc.ai.agent.subagent.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreObject {

    private String catalog;

    private String schema;

    private String objectName;

    private String objectType;

    private String objectDdl;

    private Integer relevanceScore;
}
