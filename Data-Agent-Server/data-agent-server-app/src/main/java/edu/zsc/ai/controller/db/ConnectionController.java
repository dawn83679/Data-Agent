package edu.zsc.ai.controller.db;

import edu.zsc.ai.domain.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.domain.model.dto.request.db.ConnectionCreateRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionTestResponse;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConnectionService connectionService;
    private final DbConnectionService dbConnectionService;

    @PostMapping("/test")
    public ApiResponse<ConnectionTestResponse> testConnection(@Valid @RequestBody ConnectRequest request) {
        return ApiResponse.success(connectionService.testConnection(request));
    }

    @PostMapping("/open")
    public ApiResponse<Boolean> openConnection(@RequestParam Long connectionId) {
        return ApiResponse.success(connectionService.openConnection(connectionId));
    }

    @DeleteMapping("/active/{connectionId}")
    public ApiResponse<Void> closeConnection(@PathVariable @NotNull Long connectionId) {
        connectionService.closeConnection(connectionId);
        return ApiResponse.success();
    }

    @PostMapping("/create")
    public ApiResponse<ConnectionResponse> createConnection(@Valid @RequestBody ConnectionCreateRequest request) {
        return ApiResponse.success(dbConnectionService.createConnection(request));
    }

    @GetMapping
    public ApiResponse<List<ConnectionResponse>> getConnections() {
        return ApiResponse.success(dbConnectionService.getAllConnections());
    }

    @GetMapping("/{id}")
    public ApiResponse<ConnectionResponse> getConnection(@PathVariable @NotNull Long id) {
        return ApiResponse.success(dbConnectionService.getConnectionById(id));
    }

    @PostMapping("/update")
    public ApiResponse<ConnectionResponse> updateConnection(@Valid @RequestBody ConnectionCreateRequest request) {
        return ApiResponse.success(dbConnectionService.updateConnection(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConnection(@PathVariable @NotNull Long id) {
        dbConnectionService.deleteConnection(id);
        return ApiResponse.success();
    }
}

