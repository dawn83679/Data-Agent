package edu.zsc.ai.plugin.mysql.manager;

import edu.zsc.ai.plugin.capability.ColumnManager;
import edu.zsc.ai.plugin.capability.MysqlIdentifierEscaper;
import edu.zsc.ai.plugin.constant.IsNullableEnum;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;
import edu.zsc.ai.plugin.model.metadata.ColumnMetadata;
import edu.zsc.ai.plugin.mysql.constant.MySqlTemplate;
import edu.zsc.ai.plugin.mysql.constant.MysqlColumnConstants;
import edu.zsc.ai.plugin.mysql.support.MysqlCapabilitySupport;
import edu.zsc.ai.plugin.mysql.value.MySQLDataTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class MysqlColumnManager implements ColumnManager {

    private final MysqlCapabilitySupport support;

    public MysqlColumnManager(MysqlCapabilitySupport support) {
        this.support = Objects.requireNonNull(support, "support");
    }

    @Override
    public List<ColumnMetadata> getColumns(Connection connection, String catalog, String schema, String tableOrViewName) {
        if (connection == null || StringUtils.isBlank(tableOrViewName)) {
            return List.of();
        }

        String database = support.resolveDatabase(catalog, schema);
        if (StringUtils.isBlank(database)) {
            return List.of();
        }

        String escapedDatabase = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(database);
        String escapedTable = MysqlIdentifierEscaper.getInstance().escapeStringLiteral(tableOrViewName);
        String sql = String.format(MySqlTemplate.SQL_LIST_COLUMNS, escapedDatabase, escapedTable);

        SqlCommandResult result = support.execute(connection, database, sql);
        if (!result.isSuccess()) {
            throw new RuntimeException("Failed to list columns: " + result.getErrorMessage());
        }

        List<ColumnMetadata> columns = new ArrayList<>();
        if (result.getRows() == null) {
            return columns;
        }

        for (List<Object> row : result.getRows()) {
            Object nameObject = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_NAME);
            Object positionObject = result.getValueByColumnName(row, MysqlColumnConstants.ORDINAL_POSITION);
            Object defaultObject = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_DEFAULT);
            Object nullableObject = result.getValueByColumnName(row, MysqlColumnConstants.IS_NULLABLE);
            Object dataTypeObject = result.getValueByColumnName(row, MysqlColumnConstants.DATA_TYPE);
            Object columnTypeObject = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_TYPE);
            Object columnKeyObject = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_KEY);
            Object extraObject = result.getValueByColumnName(row, MysqlColumnConstants.EXTRA);
            Object commentObject = result.getValueByColumnName(row, MysqlColumnConstants.COLUMN_COMMENT);
            Object characterLengthObject = result.getValueByColumnName(row, MysqlColumnConstants.CHARACTER_MAXIMUM_LENGTH);
            Object numericPrecisionObject = result.getValueByColumnName(row, MysqlColumnConstants.NUMERIC_PRECISION);
            Object numericScaleObject = result.getValueByColumnName(row, MysqlColumnConstants.NUMERIC_SCALE);

            String name = nameObject != null ? nameObject.toString() : "";
            if (name.isEmpty()) {
                continue;
            }

            int ordinalPosition = positionObject != null ? ((Number) positionObject).intValue() : 0;
            String defaultValue = defaultObject != null ? defaultObject.toString() : null;
            boolean nullable = IsNullableEnum.isNullable(nullableObject != null ? nullableObject.toString() : null);
            String dataType = dataTypeObject != null ? dataTypeObject.toString() : "";
            String columnType = columnTypeObject != null ? columnTypeObject.toString() : "";
            String columnKey = columnKeyObject != null ? columnKeyObject.toString() : "";
            String extra = extraObject != null ? extraObject.toString() : "";
            String remarks = commentObject != null ? commentObject.toString() : "";
            int columnSize = characterLengthObject != null ? ((Number) characterLengthObject).intValue() : 0;
            if (columnSize == 0 && numericPrecisionObject != null) {
                columnSize = ((Number) numericPrecisionObject).intValue();
            }
            int decimalDigits = numericScaleObject != null ? ((Number) numericScaleObject).intValue() : 0;

            boolean primaryKeyPart = MysqlColumnConstants.COLUMN_KEY_PRI.equals(columnKey);
            boolean autoIncrement = extra.toLowerCase().contains(MysqlColumnConstants.EXTRA_AUTO_INCREMENT);
            boolean unsigned = columnType.toLowerCase().contains("unsigned");

            columns.add(new ColumnMetadata(
                    name,
                    MySQLDataTypeEnum.toSqlType(dataType),
                    dataType,
                    columnSize,
                    decimalDigits,
                    nullable,
                    ordinalPosition,
                    remarks,
                    primaryKeyPart,
                    autoIncrement,
                    unsigned,
                    defaultValue
            ));
        }

        columns.sort(Comparator.comparingInt(ColumnMetadata::ordinalPosition));
        return columns;
    }
}
