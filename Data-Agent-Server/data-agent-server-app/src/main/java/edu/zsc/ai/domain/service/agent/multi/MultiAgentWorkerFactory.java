package edu.zsc.ai.domain.service.agent.multi;

import edu.zsc.ai.agent.MultiAgentWorker;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentRoleEnum;
import edu.zsc.ai.config.ai.MultiAgentWorkerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MultiAgentWorkerFactory {

    private final Map<String, MultiAgentWorker> schemaAnalystWorkers;
    private final Map<String, MultiAgentWorker> sqlPlannerWorkers;
    private final Map<String, MultiAgentWorker> sqlExecutorWorkers;
    private final Map<String, MultiAgentWorker> resultAnalystWorkers;

    public MultiAgentWorkerFactory(
            @Qualifier("schemaAnalystWorkers") Map<String, MultiAgentWorker> schemaAnalystWorkers,
            @Qualifier("sqlPlannerWorkers") Map<String, MultiAgentWorker> sqlPlannerWorkers,
            @Qualifier("sqlExecutorWorkers") Map<String, MultiAgentWorker> sqlExecutorWorkers,
            @Qualifier("resultAnalystWorkers") Map<String, MultiAgentWorker> resultAnalystWorkers) {
        this.schemaAnalystWorkers = schemaAnalystWorkers;
        this.sqlPlannerWorkers = sqlPlannerWorkers;
        this.sqlExecutorWorkers = sqlExecutorWorkers;
        this.resultAnalystWorkers = resultAnalystWorkers;
    }

    public MultiAgentWorker getWorker(String modelName, String language, AgentRoleEnum role) {
        String requestedModelName = StringUtils.defaultIfBlank(modelName, "qwen3-max");
        String workerKey = MultiAgentWorkerConfig.workerKey(requestedModelName);
        Map<String, MultiAgentWorker> workers = roleWorkers(role);
        MultiAgentWorker worker = workers.get(workerKey);
        if (worker == null) {
            throw new IllegalArgumentException("No MultiAgentWorker configured for role="
                    + role.getCode()
                    + ", model=" + requestedModelName
                    + ", availableModels=" + availableModels(workers));
        }
        log.debug("Resolved explicit multi-agent worker: role={}, model={}",
                role.getCode(), requestedModelName);
        return worker;
    }

    private Map<String, MultiAgentWorker> roleWorkers(AgentRoleEnum role) {
        if (role == null) {
            throw new IllegalArgumentException("Agent role is required for multi-agent worker resolution.");
        }
        return switch (role) {
            case SCHEMA_ANALYST -> schemaAnalystWorkers;
            case SQL_PLANNER -> sqlPlannerWorkers;
            case SQL_EXECUTOR -> sqlExecutorWorkers;
            case RESULT_ANALYST -> resultAnalystWorkers;
            default -> throw new IllegalArgumentException("Unsupported sub-agent role=" + role.getCode()
                    + " for " + AgentModeEnum.MULTI_AGENT.getCode());
        };
    }

    private Set<String> availableModels(Map<String, MultiAgentWorker> workers) {
        return workers.keySet().stream()
                .map(key -> key.split("::", 2)[0])
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
