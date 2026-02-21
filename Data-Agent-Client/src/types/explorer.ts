import type { DbConnection } from './connection';
import type { ExplorerNodeType, FolderName } from '../constants/explorer';

export interface ExplorerNode {
  id: string;
  name: string;
  type: ExplorerNodeType;
  connectionId?: string;
  dbConnection?: DbConnection;
  children?: ExplorerNode[];
  catalog?: string;
  schema?: string;
  /** Only set when type === 'folder' */
  folderName?: FolderName;
  /** Parent table/view name when type is column/index or folder columns/indexes */
  tableName?: string;
  /** Routine signature for function/procedure nodes; or column type display for column nodes, e.g. "int unsigned (auto increment)" */
  signature?: string;
  /** Whether the column is part of primary key (for key icon on column nodes) */
  isPrimaryKey?: boolean;
  /** Raw object name for API calls when display name differs, e.g. trigger name without " (on tableName)" */
  objectName?: string;
}
