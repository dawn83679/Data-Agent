package edu.zsc.ai.domain.model.dto.response.sys;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One organization the user belongs to, with effective role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganizationMembershipResponse {

    private Long orgId;
    private String orgCode;
    private String orgName;
    /** ADMIN or COMMON */
    private String roleCode;
}
