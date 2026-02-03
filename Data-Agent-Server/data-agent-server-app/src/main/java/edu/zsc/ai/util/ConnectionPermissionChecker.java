package edu.zsc.ai.util;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.domain.service.db.impl.ConnectionManager;
import edu.zsc.ai.util.exception.BusinessException;

/**
 * Checks that the current user is the owner of an active connection (by connectionId).
 * Use before any operation that uses connectionId (listDatabases, listSchemas, closeConnection, etc.).
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public final class ConnectionPermissionChecker {

    private ConnectionPermissionChecker() {
        // utility class
    }

    /**
     * Verify that the active connection identified by connectionId belongs to the current user.
     * Throws 404 if connection not found, 403 if connection belongs to another user.
     *
     * @param connectionId unique connection identifier
     * @throws BusinessException 404 if connection not found, 403 if not owned by current user
     */
    public static void checkConnectionOwnership(String connectionId) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        ConnectionManager.ConnectionMetadata metadata = ConnectionManager.getConnectionMetadata(connectionId);
        if (metadata == null) {
            throw BusinessException.notFound("Connection not found: " + connectionId);
        }
        if (metadata.userId() == null || !metadata.userId().equals(currentUserId)) {
            throw BusinessException.forbidden(ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);
        }
    }
}
