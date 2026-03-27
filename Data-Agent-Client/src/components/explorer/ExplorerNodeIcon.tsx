import {
  Database,
  Folder,
  Table,
  Eye,
  Rows3,
  Key,
  KeyRound,
  Hash,
  FunctionSquare,
  Zap,
} from 'lucide-react';
import { ExplorerNodeType, FolderName, RoutineIconLetter } from '../../constants/explorer';
import type { ExplorerNode } from '../../types/explorer';
import { DatabaseTypeIcon } from '../common/DatabaseTypeIcon';

const ICON_CLASS = 'w-3.5 h-3.5';

function RoutineIcon({ letter }: { letter: (typeof RoutineIconLetter)[keyof typeof RoutineIconLetter] }) {
  return (
    <span
      className={`${ICON_CLASS} flex items-center justify-center rounded-full bg-blue-500 text-white font-bold text-[10px] shrink-0`}
      style={{ minWidth: 14, minHeight: 14 }}
    >
      {letter}
    </span>
  );
}

interface ExplorerNodeIconProps {
  node: Pick<ExplorerNode, 'type' | 'folderName' | 'isPrimaryKey' | 'dbConnection'>;
}

export function ExplorerNodeIcon({ node }: ExplorerNodeIconProps) {
  const { type, folderName, isPrimaryKey, dbConnection } = node;
  const dbType = dbConnection?.dbType;

  switch (type) {
    case ExplorerNodeType.ROOT:
      return <DatabaseTypeIcon dbType={dbType} className={ICON_CLASS} fallbackClassName="text-blue-400" />;
    case ExplorerNodeType.DB:
      return <Database className={`${ICON_CLASS} text-teal-400`} />;
    case ExplorerNodeType.SCHEMA:
      return <Folder className={`${ICON_CLASS} text-amber-500`} />;
    case ExplorerNodeType.FOLDER:
      return <FolderIcon folderName={folderName} />;
    case ExplorerNodeType.TABLE:
      return <Table className={`${ICON_CLASS} text-green-400`} />;
    case ExplorerNodeType.VIEW:
      return <Eye className={`${ICON_CLASS} text-indigo-400`} />;
    case ExplorerNodeType.FUNCTION:
      return <RoutineIcon letter={RoutineIconLetter.FUNCTION} />;
    case ExplorerNodeType.PROCEDURE:
      return <RoutineIcon letter={RoutineIconLetter.PROCEDURE} />;
    case ExplorerNodeType.TRIGGER:
      return <Zap className={`${ICON_CLASS} text-orange-400`} />;
    case ExplorerNodeType.COLUMN:
      return isPrimaryKey ? (
        <Key className={`${ICON_CLASS} text-amber-500`} />
      ) : (
        <Rows3 className={`${ICON_CLASS} text-sky-400`} />
      );
    case ExplorerNodeType.KEY:
      return <Key className={`${ICON_CLASS} text-amber-500`} />;
    case ExplorerNodeType.INDEX:
      return isPrimaryKey ? (
        <Key className={`${ICON_CLASS} text-amber-500`} />
      ) : (
        <Hash className={`${ICON_CLASS} text-slate-400`} />
      );
    case ExplorerNodeType.EMPTY:
      return null;
    default:
      return <Folder className={`${ICON_CLASS} text-gray-400`} />;
  }
}

function FolderIcon({ folderName }: { folderName?: string }) {
  switch (folderName) {
    case FolderName.TABLES:
      return <Table className={`${ICON_CLASS} text-green-400`} />;
    case FolderName.VIEWS:
      return <Eye className={`${ICON_CLASS} text-indigo-400`} />;
    case FolderName.COLUMNS:
      return <Rows3 className={`${ICON_CLASS} text-sky-400`} />;
    case FolderName.KEYS:
      return <KeyRound className={`${ICON_CLASS} text-amber-500`} />;
    case FolderName.INDEXES:
      return <Hash className={`${ICON_CLASS} text-slate-400`} />;
    case FolderName.ROUTINES:
      return <FunctionSquare className={`${ICON_CLASS} text-violet-400`} />;
    case FolderName.TRIGGERS:
      return <Zap className={`${ICON_CLASS} text-orange-400`} />;
    default:
      return <Folder className={`${ICON_CLASS} text-gray-400`} />;
  }
}
