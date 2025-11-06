package edu.zsc.ai.converter;

import edu.zsc.ai.model.dto.request.ConnectRequest;
import edu.zsc.ai.plugin.model.ConnectionConfig;


/**
 * Converter for converting ConnectRequest to ConnectionConfig.
 * Handles the transformation between API request DTO and plugin configuration model.
 *
 * @author Data-Agent
 * @since 0.0.1
 */

public class ConnectionConfigConverter {

    /**
     * Convert ConnectRequest to ConnectionConfig.
     *
     * @param request connect request DTO
     * @return ConnectionConfig instance
     */
    public static ConnectionConfig convert(ConnectRequest request) {
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
}

