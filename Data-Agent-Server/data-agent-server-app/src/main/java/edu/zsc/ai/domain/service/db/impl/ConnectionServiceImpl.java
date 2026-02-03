package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.converter.db.ConnectionConfigConverter;
import edu.zsc.ai.domain.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionTestResponse;
import edu.zsc.ai.domain.model.dto.response.db.OpenConnectionResponse;
import edu.zsc.ai.common.enums.db.ConnectionTestStatus;
import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.manager.TryFirstSuccess;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.util.ConnectionPermissionChecker;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.dev33.satoken.stp.StpUtil;

import java.sql.Connection;
import java.util.List;

/**
 * Connection Service Implementation
 * Manages database connections using plugin system.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionServiceImpl implements ConnectionService {

    @Override
    public ConnectionTestResponse testConnection(ConnectRequest request) {
        long startTime = System.currentTimeMillis();
        List<ConnectionProvider> providers = DefaultPluginManager.getInstance()
                .getConnectionProviderByDbType(request.getDbType());
        ConnectionConfig config = ConnectionConfigConverter.convert(request);

        TryFirstSuccess.AttemptResult<ConnectionProvider, Connection> res;
        try {
            res = TryFirstSuccess.tryFirstSuccess(providers, p -> p.connect(config));
        } catch (UnsupportedOperationException e) {
            throw BusinessException.badRequest(
                    "Database type %s was trying to run connection test but no plugin succeeded", request.getDbType());
        }

        try {
            long ping = System.currentTimeMillis() - startTime;
            ConnectionProvider provider = res.candidate();
            Connection connection = res.result();

            String dbmsInfo = provider.getDbmsInfo(connection);
            String driverInfo = provider.getDriverInfo(connection);

            return ConnectionTestResponse.builder()
                    .status(ConnectionTestStatus.SUCCEEDED)
                    .dbmsInfo(dbmsInfo)
                    .driverInfo(driverInfo)
                    .ping(ping)
                    .build();
        } finally {
            try {
                res.candidate().closeConnection(res.result());
            } catch (Exception e) {
                log.warn("Failed to close connection", e);
            }
        }
    }

    @Override
    public OpenConnectionResponse openConnection(ConnectRequest request) {
        List<ConnectionProvider> providers = DefaultPluginManager.getInstance()
                .getConnectionProviderByDbType(request.getDbType());
        ConnectionConfig config = ConnectionConfigConverter.convert(request);

        TryFirstSuccess.AttemptResult<ConnectionProvider, Connection> res;
        try {
            res = TryFirstSuccess.tryFirstSuccess(providers, p -> p.connect(config));
        } catch (UnsupportedOperationException e) {
            throw BusinessException.badRequest(
                    "Database type %s was trying to open connection but no plugin succeeded", request.getDbType());
        }

        ConnectionProvider connectionProvider = res.candidate();
        Connection connection = res.result();

        String databaseVersion = connectionProvider.getDatabaseProductVersion(connection);

        Plugin selectedPlugin = DefaultPluginManager.getInstance().getPluginByDbTypeAndVersion(request.getDbType(), databaseVersion);

        long userId = StpUtil.getLoginIdAsLong();
        String connectionId = ConnectionManager.openConnection(request, connection, selectedPlugin.getPluginId(), userId);

        ConnectionManager.ConnectionMetadata metadata = ConnectionManager.getConnectionMetadata(connectionId);

        return OpenConnectionResponse.builder()
                .connectionId(connectionId)
                .dbType(request.getDbType())
                .host(request.getHost())
                .port(request.getPort())
                .database(request.getDatabase())
                .username(request.getUsername())
                .connected(true)
                .createdAt(metadata.createdAt())
                .build();
    }

    @Override
    public void closeConnection(String connectionId) {
        ConnectionPermissionChecker.checkConnectionOwnership(connectionId);
        ConnectionManager.closeConnection(connectionId);
    }
}

