package edu.zsc.ai.domain.model.context;

import edu.zsc.ai.api.model.request.BaseRequest;

public record DbContext(Long connectionId, String catalog, String schema) {

    public static DbContext from(BaseRequest request) {
        return new DbContext(request.getConnectionId(), request.getCatalog(), request.getSchema());
    }
}
