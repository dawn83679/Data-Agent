package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.constant.IndexTypeEnum;
import edu.zsc.ai.plugin.constant.JdbcMetaDataConstants;
import edu.zsc.ai.plugin.model.metadata.IndexMetadata;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface IndexProvider {

    default List<IndexMetadata> getIndexes(Connection connection, String catalog, String schema, String tableName) {
        try {
            Map<String, IndexBuilder> indexMap = new LinkedHashMap<>();
            DatabaseMetaData meta = connection.getMetaData();

            try (ResultSet rs = meta.getIndexInfo(catalog, schema, tableName, false, true)) {
                while (rs.next()) {
                    short type = rs.getShort(JdbcMetaDataConstants.TYPE);
                    if (type == DatabaseMetaData.tableIndexStatistic) {
                        continue;
                    }
                    String indexName = rs.getString(JdbcMetaDataConstants.INDEX_NAME);
                    if (StringUtils.isBlank(indexName)) {
                        continue;
                    }
                    boolean nonUnique = rs.getBoolean(JdbcMetaDataConstants.NON_UNIQUE);
                    int ordinalPosition = rs.getInt(JdbcMetaDataConstants.ORDINAL_POSITION);
                    String columnName = StringUtils.defaultString(rs.getString(JdbcMetaDataConstants.COLUMN_NAME));

                    String typeStr = IndexTypeEnum.fromJdbcType(type);

                    indexMap.computeIfAbsent(indexName, k -> new IndexBuilder(k, typeStr, !nonUnique))
                            .addColumn(ordinalPosition, columnName);
                }
            }

            List<IndexMetadata> result = new ArrayList<>();
            for (IndexBuilder b : indexMap.values()) {
                result.add(b.build());
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list indexes for " + tableName + ": " + e.getMessage(), e);
        }
    }
}
