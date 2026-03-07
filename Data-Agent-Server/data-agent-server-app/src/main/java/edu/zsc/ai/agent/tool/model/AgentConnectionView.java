package edu.zsc.ai.agent.tool.model;

import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;

import java.util.List;

/**
 * Slim connection view for agent consumption.
 * Omits internal fields irrelevant to query planning (driverJarPath, createdAt, updatedAt, timeout, properties).
 */
public record AgentConnectionView(Long id, String name, String dbType) {

    public static AgentConnectionView from(ConnectionResponse r) {
        return new AgentConnectionView(r.getId(), r.getName(), r.getDbType());
    }

    public static List<AgentConnectionView> fromList(List<ConnectionResponse> list) {
        return list.stream().map(AgentConnectionView::from).toList();
    }
}
