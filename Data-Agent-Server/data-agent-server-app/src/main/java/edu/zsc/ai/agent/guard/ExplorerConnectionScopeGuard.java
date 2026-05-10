package edu.zsc.ai.agent.guard;

import edu.zsc.ai.context.AgentRequestContext;

import java.util.List;

public final class ExplorerConnectionScopeGuard {

    private ExplorerConnectionScopeGuard() {
    }

    public static boolean isExplorerScope() {
        return AgentRequestContext.isExplorerScope();
    }

    public static List<Long> requireAllowedConnectionIds() {
        return AgentRequestContext.requireAllowedConnectionIds();
    }

    public static void validateConnectionAllowed(Long connectionId) {
        if (!isExplorerScope()) {
            return;
        }

        List<Long> allowedConnectionIds = requireAllowedConnectionIds();
        if (connectionId == null) {
            throw new IllegalArgumentException(
                    "Explorer 范围必须提供 connectionId；allowedConnectionIds=" + allowedConnectionIds);
        }
        if (!allowedConnectionIds.contains(connectionId)) {
            throw new IllegalArgumentException(
                    "connectionId " + connectionId + " 不在当前 explorer 任务允许范围内；allowedConnectionIds=" + allowedConnectionIds);
        }
    }
}
