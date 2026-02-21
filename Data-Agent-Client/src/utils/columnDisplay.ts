import type { ColumnMetadata } from '../services/column.service';

const COLUMN_SUFFIX_UNSIGNED = ' unsigned';
const COLUMN_SUFFIX_AUTO_INCREMENT = ' (auto increment)';

/**
 * Build column type display string from metadata.
 * e.g. "varchar(60)", "int unsigned (auto increment)"
 */
export function buildColumnDisplayType(
  typeName: string,
  columnSize: number,
  decimalDigits: number,
  isAutoIncrement: boolean,
  isUnsigned: boolean
): string {
  if (!typeName) return '';
  let s = typeName.toLowerCase();
  if (isUnsigned) s += COLUMN_SUFFIX_UNSIGNED;
  if (columnSize > 0) s += `(${columnSize}${decimalDigits > 0 ? `,${decimalDigits}` : ''})`;
  if (isAutoIncrement) s += COLUMN_SUFFIX_AUTO_INCREMENT;
  return s;
}

/** Build display type string from ColumnMetadata. Returns empty string if no type. */
export function getColumnDisplayType(col: ColumnMetadata): string {
  return buildColumnDisplayType(
    col.typeName ?? '',
    col.columnSize ?? 0,
    col.decimalDigits ?? 0,
    col.isAutoIncrement ?? false,
    col.isUnsigned ?? false
  );
}
