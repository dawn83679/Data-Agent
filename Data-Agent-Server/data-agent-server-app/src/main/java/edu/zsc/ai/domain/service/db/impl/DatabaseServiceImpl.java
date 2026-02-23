package edu.zsc.ai.domain.service.db.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.domain.service.db.DatabaseService;
import edu.zsc.ai.plugin.capability.DatabaseProvider;
import edu.zsc.ai.plugin.capability.DatabaseProvider.ColumnDefinition;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateTableOptions;
import edu.zsc.ai.plugin.capability.DatabaseProvider.CreateViewOptions;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseServiceImpl implements DatabaseService {

    private final ConnectionService connectionService;

    @Override
    public List<String> listDatabases(Long connectionId) {
        return listDatabases(connectionId, null);
    }

    @Override
    public List<String> listDatabases(Long connectionId, Long userId) {
        long uid = userId != null ? userId : StpUtil.getLoginIdAsLong();
        connectionService.openConnection(connectionId, null, null, uid);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId, uid);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());

        return provider.getDatabases(active.connection());
    }

    @Override
    public void deleteDatabase(Long connectionId, String databaseName, Long userId) {
        long uid = userId != null ? userId : StpUtil.getLoginIdAsLong();
        connectionService.openConnection(connectionId, null, null, uid);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId, uid);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());
        provider.deleteDatabase(active.connection(), active.pluginId(), databaseName);

        log.info("Database deleted successfully: connectionId={}, databaseName={}", connectionId, databaseName);
    }

    @Override
    public List<String> getCharacterSets(Long connectionId) {
        long uid = StpUtil.getLoginIdAsLong();
        connectionService.openConnection(connectionId, null, null, uid);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId, uid);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());

        return provider.getCharacterSets(active.connection());
    }

    @Override
    public List<String> getCollations(Long connectionId, String charset) {
        long uid = StpUtil.getLoginIdAsLong();
        connectionService.openConnection(connectionId, null, null, uid);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId, uid);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());

        return provider.getCollations(active.connection(), charset);
    }

    @Override
    public void createDatabase(Long connectionId, String databaseName, String charset, String collation, Long userId) {
        long uid = userId != null ? userId : StpUtil.getLoginIdAsLong();
        connectionService.openConnection(connectionId, null, null, uid);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId, uid);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());
        provider.createDatabase(active.connection(), databaseName, charset, collation);

        log.info("Database created successfully: connectionId={}, databaseName={}, charset={}, collation={}",
                connectionId, databaseName, charset, collation);
    }

    @Override
    public boolean databaseExists(Long connectionId, String databaseName, Long userId) {
        long uid = userId != null ? userId : StpUtil.getLoginIdAsLong();
        connectionService.openConnection(connectionId, null, null, uid);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId, uid);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());

        return provider.databaseExists(active.connection(), databaseName);
    }

    @Override
    public List<String> getTableEngines(Long connectionId) {
        long uid = StpUtil.getLoginIdAsLong();
        connectionService.openConnection(connectionId, null, null, uid);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId, uid);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());

        return provider.getTableEngines(active.connection());
    }

    @Override
    public void createTable(Long connectionId, String databaseName, String tableName,
                           List<ColumnDefinition> columns, CreateTableOptions options, Long userId) {
        long uid = userId != null ? userId : StpUtil.getLoginIdAsLong();
        connectionService.openConnection(connectionId, null, null, uid);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId, uid);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());
        provider.createTable(active.connection(), databaseName, tableName, columns, options);

        log.info("Table created successfully: connectionId={}, databaseName={}, tableName={}",
                connectionId, databaseName, tableName);
    }

    @Override
    public void createView(Long connectionId, String databaseName, String viewName,
                         String query, CreateViewOptions options, Long userId) {
        long uid = userId != null ? userId : StpUtil.getLoginIdAsLong();
        connectionService.openConnection(connectionId, null, null, uid);

        ConnectionManager.ActiveConnection active = ConnectionManager.getAnyOwnedActiveConnection(connectionId, uid);

        DatabaseProvider provider = DefaultPluginManager.getInstance().getDatabaseProviderByPluginId(active.pluginId());
        provider.createView(active.connection(), databaseName, viewName, query, options);

        log.info("View created successfully: connectionId={}, databaseName={}, viewName={}",
                connectionId, databaseName, viewName);
    }
}
