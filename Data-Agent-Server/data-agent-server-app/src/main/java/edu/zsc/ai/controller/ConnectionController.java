package edu.zsc.ai.controller;

import edu.zsc.ai.model.dto.request.ConnectRequest;
import edu.zsc.ai.model.dto.request.ConnectionCreateRequest;
import edu.zsc.ai.model.dto.response.ApiResponse;
import edu.zsc.ai.model.dto.response.ConnectionResponse;
import edu.zsc.ai.model.dto.response.ConnectionTestResponse;
import edu.zsc.ai.model.dto.response.OpenConnectionResponse;
import edu.zsc.ai.service.ConnectionService;
import edu.zsc.ai.service.DbConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 连接控制器
 * 提供用于数据库连接管理的 REST API 接口。
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
     * 测试数据库连接（不建立持久连接）。
     * 返回包含 DBMS 版本、驱动信息、延迟等在内的详细连接信息。
     *
     * @param request 连接测试请求
     * @return 带有详细信息的连接测试结果
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
     * 打开一个新的数据库连接，并将其存入活动连接注册表。
     * 建立可复用的持久连接，以支持后续查询。
     *
     * @param request 携带连接参数的请求
     * @return 打开连接的结果，包含 connectionId 及连接详情
     */
    @PostMapping("/open")
    public ApiResponse<OpenConnectionResponse> openConnection(@Valid @RequestBody ConnectRequest request) {
        log.info("Opening connection: dbType={}, host={}, database={}",
                request.getDbType(), request.getHost(), request.getDatabase());

        OpenConnectionResponse response = connectionService.openConnection(request);
        return ApiResponse.success(response);
    }

    /**
     * 创建一个新的数据库连接配置。
     *
     * @param request 连接创建请求
     * @return 已创建的连接配置
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
     * 获取数据库连接配置列表。
     *
     * @return 连接配置列表
     */
    @GetMapping
    public ApiResponse<List<ConnectionResponse>> getConnections() {
        log.info("Getting all connections");
        List<ConnectionResponse> connections = dbConnectionService.getAllConnections();
        return ApiResponse.success(connections);
    }

    /**
     * 根据 ID 获取数据库连接配置。
     *
     * @param id 连接配置 ID
     * @return 连接配置详情
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
     * 更新数据库连接配置。
     *
     * @param id      连接配置 ID
     * @param request 更新请求
     * @return 更新后的连接配置
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
     * 删除数据库连接配置。
     *
     * @param id 连接配置 ID
     * @return 成功响应
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
     * 关闭一个活动的数据库连接。
     *
     * @param connectionId 活动连接的唯一标识
     * @return 成功响应
     */
    @DeleteMapping("/active/{connectionId}")
    public ApiResponse<Void> closeConnection(@PathVariable String connectionId) {
        log.info("Closing connection: connectionId={}", connectionId);

        connectionService.closeConnection(connectionId);
        return ApiResponse.success();
    }
}

