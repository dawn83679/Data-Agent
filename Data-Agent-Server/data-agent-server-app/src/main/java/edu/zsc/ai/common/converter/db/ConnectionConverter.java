package edu.zsc.ai.common.converter.db;

import edu.zsc.ai.domain.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.model.entity.db.DbConnection;
import edu.zsc.ai.plugin.connection.ConnectionConfig;
import edu.zsc.ai.util.JsonUtil;
import org.springframework.beans.BeanUtils;

public class ConnectionConverter {

    private ConnectionConverter() {
    }

    /**
     * Convert DbConnection entity to ConnectionResponse DTO.
     */
    public static ConnectionResponse convertToResponse(DbConnection connection) {
        if (connection == null) {
            return null;
        }
        ConnectionResponse response = new ConnectionResponse();
        BeanUtils.copyProperties(connection, response);
        response.setProperties(JsonUtil.json2Map(connection.getProperties()));
        return response;
    }

    /**
     * Convert ConnectRequest to ConnectionConfig.
     */
    public static ConnectionConfig convertToConfig(ConnectRequest request) {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(request.getHost());
        config.setPort(request.getPort());
        config.setDatabase(request.getDatabase());
        config.setUsername(request.getUsername());
        config.setPassword(request.getPassword());
        config.setDriverJarPath(request.getDriverJarPath());
        config.setTimeout(request.getTimeout());
        config.setProperties(request.getProperties());
        return config;
    }

    /**
     * Convert DbConnection entity to ConnectionConfig.
     */
    public static ConnectionConfig convertToConfig(DbConnection entity) {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(entity.getHost());
        config.setPort(entity.getPort());
        config.setDatabase(entity.getDatabase());
        config.setUsername(entity.getUsername());
        config.setPassword(entity.getPassword());
        config.setDriverJarPath(entity.getDriverJarPath());
        config.setTimeout(entity.getTimeout());
        config.setProperties(JsonUtil.json2Map(entity.getProperties()));
        return config;
    }
}
