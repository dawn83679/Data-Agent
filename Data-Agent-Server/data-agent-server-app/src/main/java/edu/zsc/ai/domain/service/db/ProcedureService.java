package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;

import java.util.List;

public interface ProcedureService {

    List<ProcedureMetadata> getProcedures(DbContext db);

    List<ProcedureMetadata> searchProcedures(DbContext db, String procedureNamePattern);

    long countProcedures(DbContext db, String procedureNamePattern);

    String getProcedureDdl(DbContext db, String procedureName);

    void deleteProcedure(DbContext db, String procedureName);
}
