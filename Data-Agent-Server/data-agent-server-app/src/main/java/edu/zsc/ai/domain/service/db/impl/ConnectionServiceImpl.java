package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.common.converter.db.ConnectionConverter;
import edu.zsc.ai.common.enums.db.ConnectionTestStatus;
import edu.zsc.ai.domain.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionTestResponse;
import edu.zsc.ai.domain.model.entity.db.DbConnection;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ConnectionProvider;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.manager.TryFirstSuccess;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionServiceImpl implements ConnectionService {

    private final DbConnectionService dbConnectionService;

    @Override
    public ConnectionTestResponse testConnection(ConnectRequest request) {
        long startTime = System.currentTimeMillis();

        List<ConnectionProvider> providers = DefaultPluginManager.getInstance()
                .getConnectionProviderByDbType(request.getDbType());

        ConnectionConfig config = ConnectionConverter.convertToConfig(request);

        TryFirstSuccess.AttemptResult<ConnectionProvider, Connection> res =
                TryFirstSuccess.tryFirstSuccess(providers, p -> p.connect(config));

        BusinessException.assertNotNull(res,
                String.format("Database type %s was trying to run connection test but no plugin succeeded",
                        request.getDbType()));

        long ping = System.currentTimeMillis() - startTime;

        try {
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
    public Boolean openConnection(Long connectionId) {
        return openConnection(connectionId, null, null);
    }

    @Override
    public Boolean openConnection(Long connectionId, String catalog, String schema) {
        DbConnection dbConnection = dbConnectionService.getOwnedById(connectionId);

        if (ConnectionManager.getConnection(connectionId, catalog, schema).isPresent()) {
            return Boolean.TRUE;
        }

        ConnectionConfig config = ConnectionConverter.convertToConfig(dbConnection);
        if (catalog != null) {
            config.setDatabase(catalog);
        }
        if (schema != null) {
            config.setSchema(schema);
        }

        List<ConnectionProvider> providers = DefaultPluginManager.getInstance()
                .getConnectionProviderByDbType(dbConnection.getDbType());
        
        TryFirstSuccess.AttemptResult<ConnectionProvider, Connection> res =
                TryFirstSuccess.tryFirstSuccess(providers, p -> p.connect(config));
        
        BusinessException.assertNotNull(res, ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);

        ConnectionManager.ActiveConnection active = new ConnectionManager.ActiveConnection(
                res.result(),
                dbConnection.getUserId(),
                connectionId,
                dbConnection.getDbType(),
                ((Plugin) res.candidate()).getPluginId(),
                catalog,
                schema,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        ConnectionManager.registerConnection(connectionId, active);

        return Boolean.TRUE;
    }

    @Override
    public void closeConnection(Long connectionId) {
        // Check ownership before closing
        dbConnectionService.getOwnedById(connectionId);
        ConnectionManager.closeAllConnections(connectionId);
    }
}
