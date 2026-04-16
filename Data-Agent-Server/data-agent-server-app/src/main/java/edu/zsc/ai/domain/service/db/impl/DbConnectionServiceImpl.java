package edu.zsc.ai.domain.service.db.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.common.converter.db.ConnectionConverter;
import edu.zsc.ai.common.constant.ResponseCode;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.mapper.db.DbConnectionMapper;
import edu.zsc.ai.domain.mapper.sys.SysOrganizationConnectionPermissionMapper;
import edu.zsc.ai.domain.model.dto.request.db.ConnectionCreateRequest;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.model.entity.db.DbConnection;
import edu.zsc.ai.domain.model.entity.sys.SysOrganizationConnectionPermission;
import edu.zsc.ai.domain.service.db.ConnectionAccessService;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.util.JsonUtil;
import edu.zsc.ai.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DbConnectionServiceImpl extends ServiceImpl<DbConnectionMapper, DbConnection>
        implements DbConnectionService {

    private final ConnectionAccessService connectionAccessService;
    private final SysOrganizationConnectionPermissionMapper sysOrganizationConnectionPermissionMapper;

    @Override
    public DbConnection getByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        LambdaQueryWrapper<DbConnection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DbConnection::getName, name);
        return this.getOne(wrapper);
    }

    /**
     * Get connection by name for a specific user (for uniqueness check within user's connections).
     */
    private DbConnection getByNameAndUserId(String name, Long userId) {
        if (StringUtils.isBlank(name) || userId == null) {
            return null;
        }
        LambdaQueryWrapper<DbConnection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DbConnection::getName, name).eq(DbConnection::getUserId, userId);
        return this.getOne(wrapper);
    }

    @Override
    public DbConnection getOwnedById(Long id) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No userId available in RequestContext");
        }
        DbConnection connection = this.getOne(Wrappers.<DbConnection>lambdaQuery()
                .eq(DbConnection::getId, id)
                .eq(DbConnection::getUserId, userId));
        BusinessException.assertNotNull(connection, ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);
        return connection;
    }

    @Override
    public ConnectionResponse createConnection(ConnectionCreateRequest request) {
        Long currentUserId = RequestContext.getUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("No userId available in RequestContext");
        }
        DbConnection existingConnection = getByNameAndUserId(request.getName(), currentUserId);
        if (existingConnection != null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_NAME_EXISTS_MESSAGE);
        }

        DbConnection connection = new DbConnection();
        BeanUtils.copyProperties(request, connection);
        connection.setUserId(currentUserId);
        connection.setProperties(JsonUtil.map2Json(request.getProperties()));

        this.save(connection);
        if (RequestContext.isOrganizationWorkspaceEffective()) {
            Long orgId = RequestContext.getOrgId();
            if (orgId != null) {
                SysOrganizationConnectionPermission permission = sysOrganizationConnectionPermissionMapper.selectOne(
                        new LambdaQueryWrapper<SysOrganizationConnectionPermission>()
                                .eq(SysOrganizationConnectionPermission::getOrgId, orgId)
                                .eq(SysOrganizationConnectionPermission::getConnectionId, connection.getId()));
                if (permission == null) {
                    permission = new SysOrganizationConnectionPermission();
                    permission.setOrgId(orgId);
                    permission.setConnectionId(connection.getId());
                    permission.setEnabled(true);
                    permission.setGrantedBy(currentUserId);
                    permission.setCreatedAt(LocalDateTime.now());
                    permission.setUpdatedAt(LocalDateTime.now());
                    sysOrganizationConnectionPermissionMapper.insert(permission);
                } else {
                    permission.setEnabled(true);
                    permission.setGrantedBy(currentUserId);
                    permission.setUpdatedAt(LocalDateTime.now());
                    sysOrganizationConnectionPermissionMapper.updateById(permission);
                }
            }
        }
        return ConnectionConverter.convertToResponse(connection);
    }

    @Override
    public ConnectionResponse updateConnection(ConnectionCreateRequest request) {
        Long connectionId = request.getConnectionId();

        DbConnection existingConnection = this.getOwnedById(connectionId);

        Long currentUserId = RequestContext.getUserId();
        DbConnection nameConflict = getByNameAndUserId(request.getName(), currentUserId);
        if (nameConflict != null && !nameConflict.getId().equals(connectionId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_NAME_EXISTS_MESSAGE);
        }

        String originalPassword = existingConnection.getPassword();
        BeanUtils.copyProperties(request, existingConnection);
        if (StringUtils.isBlank(request.getPassword())) {
            existingConnection.setPassword(originalPassword);
        }
        existingConnection.setId(connectionId);
        existingConnection.setProperties(JsonUtil.map2Json(request.getProperties()));

        this.updateById(existingConnection);
        return ConnectionConverter.convertToResponse(existingConnection);
    }

    @Override
    public ConnectionResponse getConnectionById(Long connectionId) {
        connectionAccessService.assertReadable(connectionId);
        DbConnection connection = this.getById(connectionId);
        BusinessException.assertNotNull(connection, ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_NOT_FOUND_MESSAGE);
        return ConnectionConverter.convertToResponse(connection);
    }

    @Override
    public List<ConnectionResponse> getAllConnections() {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            throw new IllegalStateException("No userId available in RequestContext");
        }
        LambdaQueryWrapper<DbConnection> wrapper = new LambdaQueryWrapper<>();
        if (RequestContext.isPersonalWorkspaceEffective()) {
            wrapper.eq(DbConnection::getUserId, userId);
        } else {
            Long orgId = RequestContext.getOrgId();
            if (orgId == null) {
                throw new IllegalStateException("ORGANIZATION workspace requires orgId in RequestContext");
            }
            List<Long> grantedIds = sysOrganizationConnectionPermissionMapper
                    .selectList(new LambdaQueryWrapper<SysOrganizationConnectionPermission>()
                            .eq(SysOrganizationConnectionPermission::getOrgId, orgId)
                            .eq(SysOrganizationConnectionPermission::getEnabled, true))
                    .stream()
                    .map(SysOrganizationConnectionPermission::getConnectionId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            // Strict org mode: only explicitly granted connections are visible in organization workspace.
            if (grantedIds.isEmpty()) {
                wrapper.apply("1=0");
            } else {
                wrapper.in(DbConnection::getId, grantedIds);
            }
        }
        wrapper.orderByAsc(DbConnection::getId);
        return this.list(wrapper).stream()
                .map(ConnectionConverter::convertToResponse)
                .toList();
    }

    @Override
    public void deleteConnection(Long connectionId) {
        this.getOwnedById(connectionId);
        this.removeById(connectionId);
    }
}
