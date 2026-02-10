package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionTestResponse;

public interface ConnectionService {
    
    ConnectionTestResponse testConnection(ConnectRequest request);

    void closeConnection(Long connectionId);

    Boolean openConnection(Long connectionId);

    Boolean openConnection(Long connectionId, String catalog, String schema);

    /**
     * Open connection with explicit user for ownership check.
     * When {@code userId} is null, uses current login (StpUtil); use for request-thread callers.
     */
    Boolean openConnection(Long connectionId, String catalog, String schema, Long userId);
}

