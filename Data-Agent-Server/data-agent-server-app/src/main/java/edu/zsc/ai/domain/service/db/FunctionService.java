package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;

import java.util.List;

public interface FunctionService {

    List<FunctionMetadata> getFunctions(DbContext db);

    List<FunctionMetadata> searchFunctions(DbContext db, String functionNamePattern);

    long countFunctions(DbContext db, String functionNamePattern);

    String getFunctionDdl(DbContext db, String functionName);

    void deleteFunction(DbContext db, String functionName);
}
