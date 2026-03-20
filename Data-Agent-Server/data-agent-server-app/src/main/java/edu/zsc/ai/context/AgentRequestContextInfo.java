package edu.zsc.ai.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequestContextInfo {

    private String agentMode;

    private String agentType;

    private List<Long> allowedConnectionIds;

    private String modelName;

    private String language;
}
