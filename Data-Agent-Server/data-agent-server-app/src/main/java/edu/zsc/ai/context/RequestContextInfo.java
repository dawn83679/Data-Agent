package edu.zsc.ai.context;

import edu.zsc.ai.common.enums.org.OrganizationRoleEnum;
import edu.zsc.ai.common.enums.org.WorkspaceTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request Context Information
 * Contains current request context data
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RequestContextInfo {

    /**
     * Conversation ID
     */
    private Long conversationId;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Connection ID
     */
    private Long connectionId;

    /**
     * Database catalog name
     */
    private String catalog;

    /**
     * Schema name
     */
    private String schema;

    /**
     * Personal vs organization workspace (from {@code X-Workspace-Type}).
     */
    private WorkspaceTypeEnum workspaceType;

    /**
     * Current organization id when workspace is ORGANIZATION (from {@code X-Org-Id}).
     */
    private Long orgId;

    /**
     * {@code sys_organization_members.id} for the current user in {@link #orgId}.
     */
    private Long orgUserRelId;

    /**
     * Role in current organization when workspace is ORGANIZATION.
     */
    private OrganizationRoleEnum orgRole;
}
