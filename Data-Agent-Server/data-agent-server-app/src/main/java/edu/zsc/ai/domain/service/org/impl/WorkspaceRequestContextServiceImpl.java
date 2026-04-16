package edu.zsc.ai.domain.service.org.impl;

import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.common.constant.WorkspaceHttpHeaders;
import edu.zsc.ai.common.enums.org.WorkspaceTypeEnum;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.service.org.OrgAccessService;
import edu.zsc.ai.domain.service.org.OrgMemberContext;
import edu.zsc.ai.domain.service.org.WorkspaceRequestContextService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Parses {@link WorkspaceHttpHeaders} and resolves organization context when needed.
 */
@Service
public class WorkspaceRequestContextServiceImpl implements WorkspaceRequestContextService {

    @Resource
    private OrgAccessService orgAccessService;

    @Override
    public RequestContextInfo buildBaseContext(long userId, HttpServletRequest request) {
        /*
         * Organization admin APIs carry target orgId in the URL path. The UI workspace selector may still
         * point at another org (or PERSONAL). Requiring header org to match path org would block admins
         * from managing org A while workspace is org B — treat these routes as personal scope for
         * RequestContext only; authorization remains in OrganizationAdminService (assertOrgAdmin).
         */
        String servletPath = request.getServletPath();
        if (servletPath != null && servletPath.startsWith("/api/organizations")) {
            return RequestContextInfo.builder()
                    .userId(userId)
                    .workspaceType(WorkspaceTypeEnum.PERSONAL)
                    .build();
        }

        WorkspaceTypeEnum workspaceType = resolveWorkspaceType(request.getHeader(WorkspaceHttpHeaders.X_WORKSPACE_TYPE));

        if (workspaceType == WorkspaceTypeEnum.PERSONAL) {
            return RequestContextInfo.builder()
                    .userId(userId)
                    .workspaceType(WorkspaceTypeEnum.PERSONAL)
                    .build();
        }

        String orgHeader = request.getHeader(WorkspaceHttpHeaders.X_ORG_ID);
        if (!StringUtils.hasText(orgHeader)) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.WORKSPACE_ORG_ID_REQUIRED);
        }

        long orgId = parseOrgId(orgHeader.trim());
        OrgMemberContext membership = orgAccessService.loadActiveMembership(userId, orgId);

        return RequestContextInfo.builder()
                .userId(userId)
                .workspaceType(WorkspaceTypeEnum.ORGANIZATION)
                .orgId(membership.orgId())
                .orgUserRelId(membership.organizationMemberId())
                .orgRole(membership.role())
                .build();
    }

    private static WorkspaceTypeEnum resolveWorkspaceType(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return WorkspaceTypeEnum.PERSONAL;
        }
        String v = headerValue.trim().toUpperCase();
        if ("PERSONAL".equals(v)) {
            return WorkspaceTypeEnum.PERSONAL;
        }
        if ("ORGANIZATION".equals(v)) {
            return WorkspaceTypeEnum.ORGANIZATION;
        }
        throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.WORKSPACE_TYPE_INVALID);
    }

    private static long parseOrgId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw BusinessException.of(ResponseCode.PARAM_ERROR, ResponseMessageKey.WORKSPACE_ORG_ID_INVALID);
        }
    }
}
