package edu.zsc.ai.plugin.model.metadata;

public record ColumnMetadata(
        String name,
        int dataType,
        String typeName,
        int columnSize,
        int decimalDigits,
        boolean nullable,
        int ordinalPosition,
        String remarks,
        boolean isPrimaryKeyPart,
        boolean isAutoIncrement,
        boolean isUnsigned,
        String defaultValue
) {

    public ColumnMetadata(
            String name,
            int dataType,
            String typeName,
            int columnSize,
            int decimalDigits,
            boolean nullable,
            int ordinalPosition,
            String remarks
    ) {
        this(name, dataType, typeName, columnSize, decimalDigits, nullable, ordinalPosition, remarks,
                false, false, false, null);
    }
}
