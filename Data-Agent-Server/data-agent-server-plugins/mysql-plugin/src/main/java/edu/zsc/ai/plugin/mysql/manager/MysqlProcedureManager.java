package edu.zsc.ai.plugin.mysql.manager;

import edu.zsc.ai.plugin.capability.MysqlIdentifierEscaper;
import edu.zsc.ai.plugin.capability.ProcedureManager;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.ParameterInfo;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
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

public final class MysqlProcedureManager implements ProcedureManager {

    private final MysqlCapabilitySupport support;

    public MysqlProcedureManager(MysqlCapabilitySupport support) {
        this.support = Objects.requireNonNull(support, "support");
    }

    @Override
    public List<ProcedureMetadata> getProcedures(Connection connection, String catalog, String schema) {
        if (connection == null) {
            return List.of();
        }

        String database = support.resolveDatabase(catalog, schema);
        if (StringUtils.isBlank(database)) {
            return List.of();
        }

        String escapedDatabase = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(database);
        String sql = String.format(MySqlTemplate.SQL_LIST_PROCEDURES, escapedDatabase);

        SqlCommandResult result = support.execute(connection, database, sql);
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to list procedures: " + result.getErrorMessage());
        }

        Map<String, ProcedureMetadata> proceduresBySpecificName = new LinkedHashMap<>();
        if (result.getRows() != null) {
            for (List<Object> row : result.getRows()) {
                Object specificNameObject = result.getValueByColumnName(row, MysqlRoutineConstants.SPECIFIC_NAME);
                Object nameObject = result.getValueByColumnName(row, MysqlRoutineConstants.ROUTINE_NAME);

                String specificName = specificNameObject != null ? specificNameObject.toString() : "";
                String name = nameObject != null ? nameObject.toString() : "";

                if (StringUtils.isNotBlank(name) && !proceduresBySpecificName.containsKey(specificName)) {
                    proceduresBySpecificName.put(specificName, new ProcedureMetadata(name, null));
                }
            }
        }

        List<MysqlCapabilitySupport.ParamRow> parameterRows =
                support.fetchParameters(connection, database, proceduresBySpecificName.keySet());
        Map<String, List<ParameterInfo>> parametersByRoutine = support.groupParametersByRoutine(parameterRows);

        List<ProcedureMetadata> procedures = new ArrayList<>();
        for (Map.Entry<String, ProcedureMetadata> entry : proceduresBySpecificName.entrySet()) {
            ProcedureMetadata metadata = entry.getValue();
            List<ParameterInfo> parameters = parametersByRoutine.getOrDefault(entry.getKey(), List.of());
            procedures.add(new ProcedureMetadata(
                    metadata.name(),
                    parameters.isEmpty() ? null : parameters
            ));
        }
        return procedures;
    }

    @Override
    public long countProcedures(Connection connection, String catalog, String schema, String procedureNamePattern) {
        String database = support.resolveDatabase(catalog, schema);
        return support.countObjectsByName(
                connection,
                database,
                MySqlTemplate.SQL_COUNT_PROCEDURES,
                procedureNamePattern,
                MySqlTemplate.SQL_COUNT_ROUTINES_NAME_CLAUSE
        );
    }

    @Override
    public String getProcedureDdl(Connection connection, String catalog, String schema, String procedureName) {
        return support.getObjectDdl(
                connection,
                catalog,
                procedureName,
                MySqlTemplate.SQL_SHOW_CREATE_PROCEDURE,
                MysqlShowColumnConstants.CREATE_PROCEDURE,
                DatabaseObjectTypeEnum.PROCEDURE.getValue()
        );
    }

    @Override
    public void deleteProcedure(Connection connection, String catalog, String schema, String procedureName) {
        support.dropObject(
                connection,
                catalog,
                procedureName,
                MySqlTemplate.SQL_DROP_PROCEDURE,
                DatabaseObjectTypeEnum.PROCEDURE.getValue(),
                true
        );
    }
}
