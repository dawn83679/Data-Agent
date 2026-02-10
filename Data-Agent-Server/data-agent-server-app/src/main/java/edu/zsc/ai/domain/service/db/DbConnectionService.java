package edu.zsc.ai.domain.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.dto.request.db.ConnectionCreateRequest;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.model.entity.db.DbConnection;

import java.util.List;

public interface DbConnectionService extends IService<DbConnection> {

    DbConnection getByName(String name);

    DbConnection getOwnedById(Long id);

    DbConnection getOwnedById(Long id, Long userId);

    ConnectionResponse createConnection(ConnectionCreateRequest request);

    ConnectionResponse updateConnection(ConnectionCreateRequest request);

    ConnectionResponse getConnectionById(Long id);

    List<ConnectionResponse> getAllConnections();

    void deleteConnection(Long id);
}