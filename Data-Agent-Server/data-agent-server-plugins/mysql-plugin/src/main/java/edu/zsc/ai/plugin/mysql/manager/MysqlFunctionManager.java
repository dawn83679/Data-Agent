package edu.zsc.ai.plugin.mysql.manager;

import edu.zsc.ai.plugin.capability.FunctionManager;
import edu.zsc.ai.plugin.capability.MysqlIdentifierEscaper;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.FunctionMetadata;
import edu.zsc.ai.plugin.model.metadata.ParameterInfo;
import edu.zsc.ai.plugin.mysql.constant.MySqlTemplate;
import edu.zsc.ai.plugin.mysql.constant.MysqlRoutineConstants;
import edu.zsc.ai.plugin.mysql.constant.MysqlShowColumnConstants;
import edu.zsc.ai.plugin.mysql.support.MysqlCapabilitySupport;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MysqlFunctionManager implements FunctionManager {

    private final MysqlCapabilitySupport support;

    public MysqlFunctionManager(MysqlCapabilitySupport support) {
        this.support = Objects.requireNonNull(support, "support");
    }

    @Override
    public List<FunctionMetadata> getFunctions(Connection connection, String catalog, String schema) {
        if (connection == null) {
            return List.of();
        }

        String database = support.resolveDatabase(catalog, schema);
        if (StringUtils.isBlank(database)) {
            return List.of();
        }

        String escapedDatabase = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(database);
        String sql = String.format(MySqlTemplate.SQL_LIST_FUNCTIONS, escapedDatabase);

        SqlCommandResult result = support.execute(connection, database, sql);
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to list functions: " + result.getErrorMessage());
        }

        Map<String, FunctionMetadata> functionsBySpecificName = new LinkedHashMap<>();
        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                Object specificNameObject = result.getValueByColumnName(row, MysqlRoutineConstants.SPECIFIC_NAME);
                Object nameObject = result.getValueByColumnName(row, MysqlRoutineConstants.ROUTINE_NAME);
                Object returnTypeObject = result.getValueByColumnName(row, MysqlRoutineConstants.DTD_IDENTIFIER);

                String specificName = specificNameObject != null ? specificNameObject.toString() : "";
                String name = nameObject != null ? nameObject.toString() : "";
                String returnType = returnTypeObject != null ? returnTypeObject.toString().trim() : null;

                if (!name.isEmpty() && !functionsBySpecificName.containsKey(specificName)) {
                    functionsBySpecificName.put(specificName, new FunctionMetadata(name, null, returnType));
                }
            }
        }

        List<MysqlCapabilitySupport.ParamRow> parameterRows =
                support.fetchParameters(connection, database, functionsBySpecificName.keySet());
        Map<String, List<ParameterInfo>> parametersByRoutine = support.groupParametersByRoutine(parameterRows);

        List<FunctionMetadata> functions = new ArrayList<>();
        for (Map.Entry<String, FunctionMetadata> entry : functionsBySpecificName.entrySet()) {
            FunctionMetadata metadata = entry.getValue();
            List<ParameterInfo> parameters = parametersByRoutine.getOrDefault(entry.getKey(), List.of());
            functions.add(new FunctionMetadata(
                    metadata.name(),
                    parameters.isEmpty() ? null : parameters,
                    metadata.returnType()
            ));
        }
        return functions;
    }

    @Override
    public long countFunctions(Connection connection, String catalog, String schema, String functionNamePattern) {
        String database = support.resolveDatabase(catalog, schema);
        return support.countObjectsByName(
                connection,
                database,
                MySqlTemplate.SQL_COUNT_FUNCTIONS,
                functionNamePattern,
                MySqlTemplate.SQL_COUNT_ROUTINES_NAME_CLAUSE
        );
    }

    @Override
    public String getFunctionDdl(Connection connection, String catalog, String schema, String functionName) {
        return support.getObjectDdl(
                connection,
                catalog,
                functionName,
                MySqlTemplate.SQL_SHOW_CREATE_FUNCTION,
                MysqlShowColumnConstants.CREATE_FUNCTION,
                DatabaseObjectTypeEnum.FUNCTION.getValue()
        );
    }

    @Override
    public void deleteFunction(Connection connection, String catalog, String schema, String functionName) {
        support.dropObject(
                connection,
                catalog,
                functionName,
                MySqlTemplate.SQL_DROP_FUNCTION,
                DatabaseObjectTypeEnum.FUNCTION.getValue(),
                true
        );
    }
}
