package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.common.converter.db.ConnectionConverter;
import edu.zsc.ai.common.enums.db.ConnectionTestStatuEnum;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionTestResponse;
import edu.zsc.ai.domain.model.entity.db.DbConnection;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.domain.service.db.ManagedDataSourceFactory;
import edu.zsc.ai.domain.service.db.support.ConnectionManagerChain;
import edu.zsc.ai.plugin.Plugin;
import edu.zsc.ai.plugin.capability.ConnectionManager;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.domain.exception.BusinessException;
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
    private final ManagedDataSourceFactory managedDataSourceFactory;

    @Override
    public ConnectionTestResponse testConnection(ConnectRequest request) {
        long startTime = System.currentTimeMillis();

        List<ConnectionManager> managers = DefaultPluginManager.getInstance()
                .getConnectionManagerByDbType(request.getDbType());

        ConnectionConfig config = ConnectionConverter.convertToConfig(request);

        ConnectionManagerChain.ConnectionManagerHandleResult<Connection> res =
                ConnectionManagerChain.fromManagers(managers, ConnectionManager::connect).handle(config);

        BusinessException.assertNotNull(res,
                String.format("Database type %s was trying to run connection test but no plugin succeeded",
                        request.getDbType()));

        long ping = System.currentTimeMillis() - startTime;

        try {
            ConnectionManager manager = res.manager();
            Connection connection = res.result();

            String dbmsInfo = manager.getDbmsInfo(connection);
            String driverInfo = manager.getDriverInfo(connection);

            return ConnectionTestResponse.builder()
                    .status(ConnectionTestStatuEnum.SUCCEEDED)
                    .dbmsInfo(dbmsInfo)
                    .driverInfo(driverInfo)
                    .ping(ping)
                    .build();
        } finally {
            try {
                res.manager().closeConnection(res.result());
            } catch (Exception e) {
                log.warn("Failed to close connection", e);
            }
        }
    }


    @Override
    public Boolean openConnection(Long connectionId) {
        return openConnection(new DbContext(connectionId, null, null));
    }

    @Override
    public Boolean openConnection(DbContext db) {
        DbConnection dbConnection = dbConnectionService.getOwnedById(db.connectionId());
        ConnectionConfig config = ConnectionConverter.convertToConfig(dbConnection);
        if (db.catalog() != null) {
            config.setDatabase(db.catalog());
        }
        if (db.schema() != null) {
            config.setSchema(db.schema());
        }

        List<ConnectionManager> managers = DefaultPluginManager.getInstance()
                .getConnectionManagerByDbType(dbConnection.getDbType());

        ActiveConnectionRegistry.getOrCreateConnection(
                db,
                () -> buildPooledConnection(db, dbConnection, config, managers)
        );

        return Boolean.TRUE;
    }

    private ActiveConnectionRegistry.ActiveConnection buildPooledConnection(DbContext db,
                                                                     DbConnection dbConnection,
                                                                     ConnectionConfig config,
                                                                     List<ConnectionManager> managers) {
        ConnectionManagerChain.ConnectionManagerHandleResult<javax.sql.DataSource> res =
                ConnectionManagerChain.fromManagers(
                        managers,
                        (manager, ignored) -> managedDataSourceFactory.create(
                                manager,
                                config,
                                new ManagedDataSourceFactory.ManagedDataSourceRequest(
                                        db.connectionId(),
                                        dbConnection.getDbType(),
                                        db.catalog(),
                                        db.schema()
                                )
                        )
                ).handle(config);

        BusinessException.assertNotNull(res, ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);

        return new ActiveConnectionRegistry.ActiveConnection(
                res.result(),
                dbConnection.getUserId(),
                db.connectionId(),
                dbConnection.getDbType(),
                ((Plugin) res.manager()).getPluginId(),
                db.catalog(),
                db.schema(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Override
    public void closeConnection(Long connectionId) {
        // Check ownership before closing
        dbConnectionService.getOwnedById(connectionId);
        ActiveConnectionRegistry.closeAllConnections(connectionId);
    }
}
