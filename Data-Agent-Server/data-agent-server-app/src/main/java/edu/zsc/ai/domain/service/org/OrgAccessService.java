package edu.zsc.ai.domain.service.org;

import java.util.List;

/**
 * Validates organization membership and resolves the member's active role.
 */
public interface OrgAccessService {

    /**
     * Load active membership for {@code userId} in {@code orgId}.
     * Organization must exist and be enabled; member and one active role row must exist.
     *
     * @throws edu.zsc.ai.domain.exception.BusinessException if validation fails
     */
    OrgMemberContext loadActiveMembership(long userId, long orgId);

    /**
     * User ids who are active {@link edu.zsc.ai.common.enums.org.OrganizationRoleEnum#ADMIN} in {@code orgId}.
     * Used so org members can read (e.g. via AI) connections owned by org admins without a separate grant row.
     */
    List<Long> listActiveOrgAdminUserIds(long orgId);
}
