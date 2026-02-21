import { format } from 'sql-formatter';

export type SqlDialect = 'mysql' | 'postgresql' | 'plsql' | 'n1ql' | 'db2' | 'redshift' | 'spark';

/**
 * Format SQL string. Uses sql-formatter with mysql dialect by default.
 */
export function formatSql(
  sql: string,
  dialect: SqlDialect = 'mysql'
): string {
  if (!sql || !sql.trim()) return sql;
  try {
    return format(sql, { language: dialect });
  } catch {
    return sql;
  }
}
