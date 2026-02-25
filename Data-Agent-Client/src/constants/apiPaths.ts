/** Database exploration endpoints */
export const ApiPaths = {
  COLUMNS: '/columns',
  DATABASES: '/databases',
  TABLES: '/tables',
  TABLES_DDL: '/tables/ddl',
  TABLE_DATA: '/tables/data',
  VIEWS: '/views',
  VIEWS_DDL: '/views/ddl',
  VIEW_DATA: '/views/data',
  FUNCTIONS: '/functions',
  FUNCTIONS_DDL: '/functions/ddl',
  PROCEDURES: '/procedures',
  PROCEDURES_DDL: '/procedures/ddl',
  TRIGGERS: '/triggers',
  TRIGGERS_DDL: '/triggers/ddl',
  INDEXES: '/indexes',
  PRIMARY_KEYS: '/primary-keys',
} as const;

/** Authentication endpoints */
export const AuthPaths = {
  LOGIN: '/api/auth/login',
  LOGOUT: '/api/auth/logout',
  REGISTER: '/api/auth/register',
  REFRESH: '/api/auth/refresh',
  RESET_PASSWORD: '/api/auth/reset-password',
} as const;

/** Chat and AI assistant endpoints */
export const ChatPaths = {
  STREAM: '/api/chat/stream',
} as const;

/** Connection management endpoints */
export const ConnectionPaths = {
  LIST: '/connections',
  CREATE: '/connections',
  EDIT: '/connections/:id',
  DELETE: '/connections/:id',
  TEST: '/connections/test',
} as const;

/** SQL execution endpoints */
export const SqlPaths = {
  EXECUTE: '/db/sql/execute',
} as const;
