package edu.zsc.ai.domain.service.db.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.common.enums.org.OrganizationRoleEnum;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.exception.BusinessException;
import edu.zsc.ai.domain.mapper.db.DbConnectionMapper;
import edu.zsc.ai.domain.mapper.sys.SysOrganizationConnectionPermissionMapper;
import edu.zsc.ai.domain.model.entity.db.DbConnection;
import edu.zsc.ai.domain.model.entity.sys.SysOrganizationConnectionPermission;
import edu.zsc.ai.domain.service.db.ConnectionAccessService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ConnectionAccessServiceImpl implements ConnectionAccessService {

    @Resource
    private DbConnectionMapper dbConnectionMapper;
    @Resource
    private SysOrganizationConnectionPermissionMapper sysOrganizationConnectionPermissionMapper;

    @Override
    public boolean isOwner(long connectionId, long userId) {
        DbConnection conn = dbConnectionMapper.selectById(connectionId);
        return conn != null && Objects.equals(conn.getUserId(), userId);
    }

    @Override
    public boolean canRead(long connectionId) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return false;
        }
        DbConnection conn = dbConnectionMapper.selectById(connectionId);
        if (conn == null) {
            return false;
        }
        // Chat session embeds the same id list as getAllConnections; tool threads may lose RequestContext.org.
        List<Long> agentReadable = AgentRequestContext.getReadableConnectionIds();
        if (CollectionUtils.isNotEmpty(agentReadable)) {
            return agentReadable.stream().anyMatch(id -> Objects.equals(id, connectionId));
        }
        Long orgId = RequestContext.getOrgId();
        if (RequestContext.isPersonalWorkspaceEffective()) {
            return Objects.equals(conn.getUserId(), userId);
        }
        // Organization (or org-like) context: do not treat "I own this connection" as sufficient;
        // otherwise personal-workspace connections leak into org workspace for COMMON members.
        if (orgId == null) {
            return false;
        }
        Long count = sysOrganizationConnectionPermissionMapper.selectCount(
                new LambdaQueryWrapper<SysOrganizationConnectionPermission>()
                        .eq(SysOrganizationConnectionPermission::getOrgId, orgId)
                        .eq(SysOrganizationConnectionPermission::getConnectionId, connectionId)
                        .eq(SysOrganizationConnectionPermission::getEnabled, true));
        if (count != null && count > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void assertReadable(long connectionId) {
        if (!canRead(connectionId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);
        }
    }

    @Override
    public void assertWritableForCurrentWorkspace(long connectionId) {
        assertReadable(connectionId);
        if (RequestContext.isOrganizationWorkspaceEffective()
                && RequestContext.getOrgRole() == OrganizationRoleEnum.COMMON) {
            throw new BusinessException(ResponseCode.FORBIDDEN, ResponseMessageKey.WORKSPACE_COMMON_WRITE_FORBIDDEN);
        }
    }

    @Override
    public void assertWorkbenchApiAllowed() {
        if (RequestContext.isOrganizationWorkspaceEffective()
                && RequestContext.getOrgRole() == OrganizationRoleEnum.COMMON) {
            throw new BusinessException(ResponseCode.FORBIDDEN, ResponseMessageKey.WORKSPACE_COMMON_WORKBENCH_FORBIDDEN);
        }
    }
}
