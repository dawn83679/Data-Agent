package edu.zsc.ai.controller;

import edu.zsc.ai.model.dto.request.ConnectRequest;
import edu.zsc.ai.model.dto.request.ConnectionCreateRequest;
import edu.zsc.ai.model.dto.response.ApiResponse;
import edu.zsc.ai.model.dto.response.ConnectionResponse;
import edu.zsc.ai.model.dto.response.ConnectionTestResponse;
import edu.zsc.ai.model.dto.response.OpenConnectionResponse;
import edu.zsc.ai.service.ConnectionService;
import edu.zsc.ai.service.DbConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Connection Management", description = "Database connection management APIs")
@Slf4j
@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;
    private final DbConnectionService dbConnectionService;

    @Operation(summary = "Test Connection", description = "Test database connection without establishing persistent connection")
    @PostMapping("/test")
    public ApiResponse<ConnectionTestResponse> testConnection(
            @Valid @RequestBody ConnectRequest request) {
        log.info("Testing connection: dbType={}, host={}, database={}",
                request.getDbType(), request.getHost(), request.getDatabase());

        ConnectionTestResponse response = connectionService.testConnection(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Open Connection", description = "Open a new database connection and store it in the active connections registry")
    @PostMapping("/open")
    public ApiResponse<OpenConnectionResponse> openConnection(@Valid @RequestBody ConnectRequest request) {
        log.info("Opening connection: dbType={}, host={}, database={}",
                request.getDbType(), request.getHost(), request.getDatabase());

        OpenConnectionResponse response = connectionService.openConnection(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Create Connection", description = "Create a new database connection configuration")
    @PostMapping("/create")
    public ApiResponse<ConnectionResponse> createConnection(
            @Valid @RequestBody ConnectionCreateRequest request) {
        log.info("Creating connection: name={}, dbType={}, host={}",
                request.getName(), request.getDbType(), request.getHost());
        ConnectionResponse response = dbConnectionService.createConnection(request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "Get All Connections", description = "Get list of all database connections")
    @GetMapping
    public ApiResponse<List<ConnectionResponse>> getConnections() {
        log.info("Getting all connections");
        List<ConnectionResponse> connections = dbConnectionService.getAllConnections();
        return ApiResponse.success(connections);
    }

    @Operation(summary = "Get Connection by ID", description = "Get database connection details by ID")
    @GetMapping("/{id}")
    public ApiResponse<ConnectionResponse> getConnection(
            @Parameter(description = "Connection ID") @PathVariable Long id) {
        log.info("Getting connection: id={}", id);

        try {
            ConnectionResponse response = dbConnectionService.getConnectionById(id);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "Update Connection", description = "Update database connection configuration")
    @PutMapping("/{id}")
    public ApiResponse<ConnectionResponse> updateConnection(
            @Parameter(description = "Connection ID") @PathVariable Long id,
            @Valid @RequestBody ConnectionCreateRequest request) {
        log.info("Updating connection: id={}, name={}", id, request.getName());

        try {
            ConnectionResponse response = dbConnectionService.updateConnection(id, request);
            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "Delete Connection", description = "Delete database connection configuration")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConnection(
            @Parameter(description = "Connection ID") @PathVariable Long id) {
        log.info("Deleting connection: id={}", id);

        try {
            dbConnectionService.deleteConnection(id);
            return ApiResponse.success();
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Operation(summary = "Close Active Connection", description = "Close an active database connection")
    @DeleteMapping("/active/{connectionId}")
    public ApiResponse<Void> closeConnection(
            @Parameter(description = "Connection ID") @PathVariable String connectionId) {
        log.info("Closing connection: connectionId={}", connectionId);

        connectionService.closeConnection(connectionId);
        return ApiResponse.success();
    }
}

