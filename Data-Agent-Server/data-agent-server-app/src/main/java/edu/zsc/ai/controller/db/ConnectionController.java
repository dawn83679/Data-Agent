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
 * REST controller that exposes CRUD endpoints for database connections.
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
     * Test a database connection without creating a persistent session.
     * The response includes DBMS version, driver metadata, latency, etc.
     *
     * @param request payload describing how to connect to the database
     * @return enriched diagnostic results for the attempted connection
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
     * Open a long-lived connection and register it so subsequent calls can reuse it.
     *
     * @param request connection properties to establish the session
     * @return result containing the assigned connectionId and metadata
     */
    @PostMapping("/open")
    public ApiResponse<OpenConnectionResponse> openConnection(@Valid @RequestBody ConnectRequest request) {
        log.info("Opening connection: dbType={}, host={}, database={}",
                request.getDbType(), request.getHost(), request.getDatabase());

        OpenConnectionResponse response = connectionService.openConnection(request);
        return ApiResponse.success(response);
    }

    /**
     * Persist a new reusable connection profile.
     *
     * @param request desired connection attributes
     * @return the stored connection profile
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
     * Retrieve every stored connection profile.
     *
     * @return list of connection profiles
     */
    @GetMapping
    public ApiResponse<List<ConnectionResponse>> getConnections() {
        log.info("Getting all connections");
        List<ConnectionResponse> connections = dbConnectionService.getAllConnections();
        return ApiResponse.success(connections);
    }

    /**
     * Fetch a single connection profile by its id.
     *
     * @param id connection profile identifier
     * @return connection profile details
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
     * Update the properties of an existing connection profile.
     *
     * @param id      connection profile identifier
     * @param request updated connection attributes
     * @return connection profile after the update
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
     * Delete a stored connection profile.
     *
     * @param id connection profile identifier
     * @return success response wrapper
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
     * Terminate an active registered connection.
     *
     * @param connectionId identifier assigned when the connection was opened
     * @return success response wrapper
     */
    @DeleteMapping("/active/{connectionId}")
    public ApiResponse<Void> closeConnection(@PathVariable String connectionId) {
        log.info("Closing connection: connectionId={}", connectionId);

        connectionService.closeConnection(connectionId);
        return ApiResponse.success();
    }
}

