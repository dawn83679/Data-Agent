export enum SqlDialectEnum {
  MYSQL = 'mysql',
  POSTGRESQL = 'postgresql',
  PLSQL = 'plsql',
  N1QL = 'n1ql',
  DB2 = 'db2',
  REDSHIFT = 'redshift',
  SPARK = 'spark',
}

export type SqlDialect = 'mysql' | 'postgresql' | 'plsql' | 'n1ql' | 'db2' | 'redshift' | 'spark';
