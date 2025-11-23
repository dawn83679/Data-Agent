package edu.zsc.ai.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.model.dto.request.db.ConnectionCreateRequest;
import edu.zsc.ai.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.model.entity.db.DbConnection;

import java.util.List;

/**
 * Database Connection Service Interface
 * Provides CRUD operations for database connection information.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public interface DbConnectionService extends IService<DbConnection> {

    /**
     * Get database connection by name
     *
     * @param name connection name
     * @return database connection entity
     */
    DbConnection getByName(String name);

    
    /**
     * Create a new database connection
     *
     * @param request connection creation request
     * @return created connection response
     */
    ConnectionResponse createConnection(ConnectionCreateRequest request);

    /**
     * Update database connection
     *
     * @param id      connection ID
     * @param request update request
     * @return updated connection response
     */
    ConnectionResponse updateConnection(Long id, ConnectionCreateRequest request);

    /**
     * Get database connection by ID and convert to response
     *
     * @param id connection ID
     * @return connection response
     */
    ConnectionResponse getConnectionById(Long id);

    /**
     * Get list of all connection responses
     *
     * @return list of connection responses
     */
    List<ConnectionResponse> getAllConnections();

    /**
     * Delete database connection by ID
     *
     * @param id connection ID
     */
    void deleteConnection(Long id);
}