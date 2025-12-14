package edu.zsc.ai.service.impl.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.zsc.ai.mapper.db.DbConnectionMapper;
import edu.zsc.ai.model.dto.request.db.ConnectionCreateRequest;
import edu.zsc.ai.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.model.entity.db.DbConnection;
import edu.zsc.ai.service.db.DbConnectionService;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

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

    
    @Override
    public ConnectionResponse createConnection(ConnectionCreateRequest request) {
        // Check if connection name already exists
        DbConnection existingConnection = getByName(request.getName());
        if (existingConnection != null) {
            throw new IllegalArgumentException("Connection name already exists: " + request.getName());
        }

        // Convert request to entity
        DbConnection connection = new DbConnection();
        BeanUtils.copyProperties(request, connection);

        // Convert properties Map to JSON string using JsonUtil
        connection.setProperties(JsonUtil.map2Json(request.getProperties()));

        // Save connection
        this.save(connection);

        // Convert to response
        return convertToResponse(connection);
    }

    @Override
    public ConnectionResponse updateConnection(Long id, ConnectionCreateRequest request) {
        DbConnection existingConnection = this.getById(id);
        if (existingConnection == null) {
            throw new IllegalArgumentException("Connection not found: " + id);
        }

        // Check if name conflicts with other connections
        DbConnection nameConflict = getByName(request.getName());
        if (nameConflict != null && !nameConflict.getId().equals(id)) {
            throw new IllegalArgumentException("Connection name already exists: " + request.getName());
        }

        // Update connection
        BeanUtils.copyProperties(request, existingConnection);
        existingConnection.setId(id);

        // Convert properties Map to JSON string using JsonUtil
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
        return convertToResponse(connection);
    }

    @Override
    public List<ConnectionResponse> getAllConnections() {
        LambdaQueryWrapper<DbConnection> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(DbConnection::getId);
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