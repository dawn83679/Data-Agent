import { format } from 'sql-formatter';
import { SqlDialectEnum, type SqlDialect } from '../constants/sqlDialect';

/**
 * Format SQL string using sql-formatter
 * @param sql - The SQL string to format
 * @param dialect - The SQL dialect (default: mysql)
 * @returns Formatted SQL string, or original if formatting fails
 */
export function formatSql(
  sql: string,
  dialect: SqlDialect = SqlDialectEnum.MYSQL
): string {
  if (!sql || !sql.trim()) return sql;
  try {
    return format(sql, { language: dialect });
  } catch {
    return sql;
  }
}
