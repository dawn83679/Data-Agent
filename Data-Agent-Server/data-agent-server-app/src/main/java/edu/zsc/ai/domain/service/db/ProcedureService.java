package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;

import java.util.List;

public interface ProcedureService {

    List<ProcedureMetadata> getProcedures(Long connectionId, String catalog, String schema, Long userId);

    String getProcedureDdl(Long connectionId, String catalog, String schema, String procedureName, Long userId);

    void deleteProcedure(Long connectionId, String catalog, String schema, String procedureName, Long userId);
}
