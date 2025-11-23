package edu.zsc.ai.service.db;

import edu.zsc.ai.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.model.dto.response.db.ConnectionTestResponse;
import edu.zsc.ai.model.dto.response.db.OpenConnectionResponse;

/**
 * Connection Service Interface
 * Provides database connection management operations.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public interface ConnectionService {
    
    /**
     * Test database connection without establishing persistent connection.
     * Returns detailed connection information including DBMS version, driver info, ping time, etc.
     *
     * @param request test connection request
     * @return connection test response with detailed information
     */
    ConnectionTestResponse testConnection(ConnectRequest request);
    
    /**
     * Open a new database connection and store it in the active connections registry.
     * Establishes a persistent connection that can be reused for queries.
     *
     * @param request connection request with connection parameters
     * @return open connection response with connectionId and connection details
     */
    OpenConnectionResponse openConnection(ConnectRequest request);
    
    /**
     * Close an active database connection and release resources.
     *
     * @param connectionId unique connection identifier
     */
    void closeConnection(String connectionId);
}

