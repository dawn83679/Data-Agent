package edu.zsc.ai.domain.service.db.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.common.constant.ResponseMessageKey;
import edu.zsc.ai.domain.mapper.db.DbConnectionMapper;
import edu.zsc.ai.domain.model.dto.request.db.ConnectionCreateRequest;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.model.entity.db.DbConnection;
import edu.zsc.ai.domain.service.db.DbConnectionService;
import edu.zsc.ai.util.JsonUtil;
import edu.zsc.ai.util.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Database Connection Service Implementation
 *
 * @author Data-Agent
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbConnectionServiceImpl extends ServiceImpl<DbConnectionMapper, DbConnection>
        implements DbConnectionService {


    @Override
    public DbConnection getByName(String name) {
        if (!StringUtils.isNotBlank(name)) {
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
        if (!StringUtils.isNotBlank(name) || userId == null) {
            return null;
        }
        LambdaQueryWrapper<DbConnection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DbConnection::getName, name).eq(DbConnection::getUserId, userId);
        return this.getOne(wrapper);
    }

    private void requireConnectionOwnedByCurrentUser(DbConnection connection) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (connection.getUserId() == null || !connection.getUserId().equals(currentUserId)) {
            throw BusinessException.forbidden(ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);
        }
    }

    
    @Override
    public ConnectionResponse createConnection(ConnectionCreateRequest request) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        DbConnection existingConnection = getByNameAndUserId(request.getName(), currentUserId);
        if (existingConnection != null) {
            throw new IllegalArgumentException("Connection name already exists: " + request.getName());
        }

        DbConnection connection = new DbConnection();
        BeanUtils.copyProperties(request, connection);
        connection.setUserId(currentUserId);
        connection.setProperties(JsonUtil.map2Json(request.getProperties()));

        this.save(connection);
        return convertToResponse(connection);
    }

    @Override
    public ConnectionResponse updateConnection(Long id, ConnectionCreateRequest request) {
        DbConnection existingConnection = this.getById(id);
        if (existingConnection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }
        requireConnectionOwnedByCurrentUser(existingConnection);

        long currentUserId = StpUtil.getLoginIdAsLong();
        DbConnection nameConflict = getByNameAndUserId(request.getName(), currentUserId);
        if (nameConflict != null && !nameConflict.getId().equals(id)) {
            throw new IllegalArgumentException("Connection name already exists: " + request.getName());
        }

        BeanUtils.copyProperties(request, existingConnection);
        existingConnection.setId(id);
        existingConnection.setProperties(JsonUtil.map2Json(request.getProperties()));

        this.updateById(existingConnection);
        return convertToResponse(existingConnection);
    }

    @Override
    public ConnectionResponse getConnectionById(Long id) {
        DbConnection connection = this.getById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }
        requireConnectionOwnedByCurrentUser(connection);
        return convertToResponse(connection);
    }

    @Override
    public List<ConnectionResponse> getAllConnections() {
        long currentUserId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<DbConnection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DbConnection::getUserId, currentUserId).orderByAsc(DbConnection::getId);
        List<DbConnection> connections = this.list(wrapper);
        return connections.stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public void deleteConnection(Long id) {
        DbConnection connection = this.getById(id);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }
        requireConnectionOwnedByCurrentUser(connection);
        this.removeById(id);
    }

    /**
     * Convert entity to response DTO
     */
    private ConnectionResponse convertToResponse(DbConnection connection) {
        ConnectionResponse response = new ConnectionResponse();
        BeanUtils.copyProperties(connection, response);

        // Convert properties JSON string to Map using JsonUtil
        response.setProperties(JsonUtil.json2Map(connection.getProperties()));

        return response;
    }
}