package edu.zsc.ai.common.converter.db;

import edu.zsc.ai.domain.model.dto.request.db.ConnectRequest;
import edu.zsc.ai.domain.service.db.impl.ConnectionManager;

import java.time.LocalDateTime;

/**
 * Converter for converting ConnectRequest to ConnectionMetadata.
 * Handles the transformation between API request DTO and connection metadata.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public class ConnectionMetadataConverter {

    /**
     * Convert ConnectRequest to ConnectionMetadata.
     *
     * @param request connect request DTO
     * @param pluginId the plugin ID used to establish this connection
     * @return ConnectionMetadata instance
     */
    public static ConnectionManager.ConnectionMetadata convert(ConnectRequest request, String pluginId) {
        LocalDateTime now = LocalDateTime.now();
        return new ConnectionManager.ConnectionMetadata(
                request.getDbType(),
                request.getHost(),
                request.getPort(),
                request.getDatabase(),
                request.getUsername(),
                pluginId,
                now,
                now
        );
    }
}

