package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface SchemaProvider {

    default List<String> getSchemas(Connection connection, String catalog) {
        try {
            List<String> list = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getSchemas(catalog, null)) {
                while (rs.next()) {
                    String name = rs.getString(JdbcMetaDataConstants.TABLE_SCHEM);
                    if (StringUtils.isNotBlank(name)) {
                        list.add(name);
                    }
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list schemas: " + e.getMessage(), e);
        }
    }
}
