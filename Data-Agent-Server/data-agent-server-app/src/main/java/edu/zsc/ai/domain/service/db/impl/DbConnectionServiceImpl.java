package edu.zsc.ai.domain.service.db.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.common.converter.db.ConnectionConverter;
import edu.zsc.ai.common.constant.ResponseCode;
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

    @Override
    public DbConnection getOwnedById(Long id) {
        DbConnection connection = this.getOne(Wrappers.<DbConnection>lambdaQuery()
                .eq(DbConnection::getId, id)
                .eq(DbConnection::getUserId, StpUtil.getLoginIdAsLong()));
        BusinessException.assertNotNull(connection, ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_ACCESS_DENIED_MESSAGE);
        return connection;
    }

    @Override
    public ConnectionResponse createConnection(ConnectionCreateRequest request) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        DbConnection existingConnection = getByNameAndUserId(request.getName(), currentUserId);
        if (existingConnection != null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_NAME_EXISTS_MESSAGE);
        }

        DbConnection connection = new DbConnection();
        BeanUtils.copyProperties(request, connection);
        connection.setUserId(currentUserId);
        connection.setProperties(JsonUtil.map2Json(request.getProperties()));

        this.save(connection);
        return ConnectionConverter.convertToResponse(connection);
    }

    @Override
    public ConnectionResponse updateConnection(ConnectionCreateRequest request) {
        Long connectionId = request.getConnectionId();
        long currentUserId = StpUtil.getLoginIdAsLong();

        DbConnection existingConnection = this.getOwnedById(connectionId);

        DbConnection nameConflict = getByNameAndUserId(request.getName(), currentUserId);
        if (nameConflict != null && !nameConflict.getId().equals(connectionId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, ResponseMessageKey.CONNECTION_NAME_EXISTS_MESSAGE);
        }

        BeanUtils.copyProperties(request, existingConnection);
        existingConnection.setId(connectionId);
        existingConnection.setProperties(JsonUtil.map2Json(request.getProperties()));

        this.updateById(existingConnection);
        return ConnectionConverter.convertToResponse(existingConnection);
    }

    @Override
    public ConnectionResponse getConnectionById(Long connectionId) {
        DbConnection connection = this.getOwnedById(connectionId);
        return ConnectionConverter.convertToResponse(connection);
    }

    @Override
    public List<ConnectionResponse> getAllConnections() {
        long currentUserId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<DbConnection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DbConnection::getUserId, currentUserId).orderByAsc(DbConnection::getId);
        List<DbConnection> connections = this.list(wrapper);
        return connections.stream()
                .map(ConnectionConverter::convertToResponse)
                .toList();
    }

    @Override
    public void deleteConnection(Long connectionId) {
        this.getOwnedById(connectionId);
        this.removeById(connectionId);
    }
}