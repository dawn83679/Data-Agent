package edu.zsc.ai.controller.db;

import edu.zsc.ai.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.model.dto.request.db.ConnectionCreateRequest;
import edu.zsc.ai.model.dto.response.base.ApiResponse;
import edu.zsc.ai.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.model.dto.response.db.ConnectionTestResponse;
import edu.zsc.ai.model.dto.response.db.OpenConnectionResponse;
import edu.zsc.ai.service.ConnectionService;
import edu.zsc.ai.service.DbConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Connection Controller
 * Provides REST API endpoints for database connection management.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;
    private final DbConnectionService dbConnectionService;

    /**
     * Test database connection without establishing persistent connection.
     * Returns detailed connection information including DBMS version, driver info, ping time, etc.
     *
     * @param request connect request
     * @return connection test response with detailed information
     */
    @PostMapping("/test")
    public ApiResponse<ConnectionTestResponse> testConnection(
            @Valid @RequestBody ConnectRequest request) {
        log.info("Testing connection: dbType={}, host={}, database={}",
                request.getDbType(), request.getHost(), request.getDatabase());

        ConnectionTestResponse response = connectionService.testConnection(request);
        return ApiResponse.success(response);
    }

    /**
     * Open a new database connection and store it in the active connections registry.
     * Establishes a persistent connection that can be reused for queries.
     *
     * @param request connection request with connection parameters
     * @return open connection response with connectionId and connection details
     */
    @PostMapping("/open")
    public ApiResponse<OpenConnectionResponse> openConnection(@Valid @RequestBody ConnectRequest request) {
        log.info("Opening connection: dbType={}, host={}, database={}",
                request.getDbType(), request.getHost(), request.getDatabase());

        OpenConnectionResponse response = connectionService.openConnection(request);
        return ApiResponse.success(response);
    }

    /**
     * Create a new database connection.
     *
     * @param request connection creation request
     * @return created connection response
     */
    @PostMapping("/create")
    public ApiResponse<ConnectionResponse> createConnection(
            @Valid @RequestBody ConnectionCreateRequest request) {
        log.info("Creating connection: name={}, dbType={}, host={}",
                request.getName(), request.getDbType(), request.getHost());
        ConnectionResponse response = dbConnectionService.createConnection(request);
        return ApiResponse.success(response);
    }

    /**
     * Get list of database connections.
     *
     * @return connection list
     */
    @GetMapping
    public ApiResponse<List<ConnectionResponse>> getConnections() {
        log.info("Getting all connections");
        List<ConnectionResponse> connections = dbConnectionService.getAllConnections();
        return ApiResponse.success(connections);
    }

    /**
     * Get database connection by ID.
     *
     * @param id connection ID
     * @return connection response
     */
    @GetMapping("/{id}")
    public ApiResponse<ConnectionResponse> getConnection(@PathVariable Long id) {
        log.info("Getting connection: id={}", id);

        try {
            ConnectionResponse response = dbConnectionService.getConnectionById(id);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Update database connection.
     *
     * @param id      connection ID
     * @param request update request
     * @return updated connection response
     */
    @PutMapping("/{id}")
    public ApiResponse<ConnectionResponse> updateConnection(
            @PathVariable Long id,
            @Valid @RequestBody ConnectionCreateRequest request) {
        log.info("Updating connection: id={}, name={}", id, request.getName());

        try {
            ConnectionResponse response = dbConnectionService.updateConnection(id, request);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Delete database connection.
     *
     * @param id connection ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConnection(@PathVariable Long id) {
        log.info("Deleting connection: id={}", id);

        try {
            dbConnectionService.deleteConnection(id);
            return ApiResponse.success();
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Close an active database connection.
     *
     * @param connectionId unique connection identifier
     * @return success response
     */
    @DeleteMapping("/active/{connectionId}")
    public ApiResponse<Void> closeConnection(@PathVariable String connectionId) {
        log.info("Closing connection: connectionId={}", connectionId);

        connectionService.closeConnection(connectionId);
        return ApiResponse.success();
    }
}

