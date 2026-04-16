package edu.zsc.ai.common.constant;

/**
 * HTTP headers for workspace (personal vs organization) context.
 */
public final class WorkspaceHttpHeaders {

    private WorkspaceHttpHeaders() {
    }

    /**
     * Value: PERSONAL | ORGANIZATION
     */
    public static final String X_WORKSPACE_TYPE = "X-Workspace-Type";

    /**
     * Required when workspace type is ORGANIZATION; current organization id.
     */
    public static final String X_ORG_ID = "X-Org-Id";
}
