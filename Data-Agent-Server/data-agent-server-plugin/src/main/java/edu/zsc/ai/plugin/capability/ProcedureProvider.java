package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.model.metadata.ProcedureMetadata;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface ProcedureProvider {

    default List<ProcedureMetadata> getProcedures(Connection connection, String catalog, String schema) {
        try {
            List<ProcedureMetadata> list = new ArrayList<>();
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet rs = meta.getProcedures(catalog, schema, null)) {
                while (rs.next()) {
                    short procType = rs.getShort(JdbcMetaDataConstants.PROCEDURE_TYPE);
                    if (procType == DatabaseMetaData.procedureResultUnknown
                            || procType == DatabaseMetaData.procedureNoResult
                            || procType == DatabaseMetaData.procedureReturnsResult) {
                        String name = rs.getString(JdbcMetaDataConstants.PROCEDURE_NAME);
                        if (StringUtils.isNotBlank(name)) {
                            list.add(new ProcedureMetadata(name));
                        }
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list procedures: " + e.getMessage(), e);
        }
    }

    default String getProcedureDdl(Connection connection, String catalog, String schema, String procedureName) {
        throw new UnsupportedOperationException("Plugin does not support getting procedure DDL");
    }
}
