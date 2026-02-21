package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;

import java.util.List;

public interface FunctionService {

    List<FunctionMetadata> listFunctions(Long connectionId, String catalog, String schema, Long userId);

    String getFunctionDdl(Long connectionId, String catalog, String schema, String functionName, Long userId);

    void deleteFunction(Long connectionId, String catalog, String schema, String functionName, Long userId);
}
