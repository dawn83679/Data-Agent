package edu.zsc.ai.domain.service.org;

import edu.zsc.ai.common.enums.org.OrganizationRoleEnum;

/**
 * Resolved organization membership for the current request.
 */
public record OrgMemberContext(long orgId, long organizationMemberId, OrganizationRoleEnum role) {
}
