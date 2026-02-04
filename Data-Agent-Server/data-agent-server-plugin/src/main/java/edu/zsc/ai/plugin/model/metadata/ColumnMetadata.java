package edu.zsc.ai.plugin.model.metadata;

/**
 * Column metadata returned by {@link edu.zsc.ai.plugin.capability.ColumnProvider}.
 * Immutable value object for column information.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public record ColumnMetadata(
        String name,
        int dataType,
        String typeName,
        int columnSize,
        int decimalDigits,
        boolean nullable,
        int ordinalPosition,
        String remarks
) {
}
