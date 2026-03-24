import { Play, Plus, Minus, FileText, ChevronDown, RefreshCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '../ui/Button';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { TransactionModeSelector } from './TransactionModeSelector';
import { TransactionMode, IsolationLevel } from '../../constants/transactionSettings';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/DropdownMenu';

interface TableDataToolbarProps {
  loading: boolean;
  isTable: boolean;
  viewMode: 'grid' | 'transpose';
  hasRowSelection: boolean;
  deletePending: boolean;
  txMode: TransactionMode;
  isolationLevel: IsolationLevel;
  displayDbLabel: string;
  connectionName?: string | null;
  databases: string[];
  loadingDatabases: boolean;
  onRun: () => void;
  onRefresh: () => void;
  onTransactionModeChange: (mode: TransactionMode) => void;
  onIsolationLevelChange: (level: IsolationLevel) => void;
  onAddRow: () => void;
  onDeleteRow: () => void;
  onOpenDdl: () => void;
  onDatabaseChange: (db: string) => void;
  onViewModeChange: (mode: 'grid' | 'transpose') => void;
}

export function TableDataToolbar({
  loading,
  isTable,
  viewMode,
  hasRowSelection,
  deletePending,
  txMode,
  isolationLevel,
  displayDbLabel,
  connectionName,
  databases,
  loadingDatabases,
  onRun,
  onRefresh,
  onTransactionModeChange,
  onIsolationLevelChange,
  onAddRow,
  onDeleteRow,
  onOpenDdl,
  onDatabaseChange,
  onViewModeChange,
}: TableDataToolbarProps) {
  const { t } = useTranslation();
  const isTransposeMode = viewMode === 'transpose';

  return (
    <div className="h-8 flex items-center px-2 theme-bg-main border-b theme-border text-[10px] theme-text-secondary shrink-0 gap-1">
      <Button
        variant="ghost"
        size="icon"
        onClick={onRun}
        disabled={loading}
        title={t(I18N_KEYS.COMMON.EXECUTE_QUERY)}
        className="h-6 w-6"
      >
        <Play className="w-3.5 h-3.5 fill-current text-green-500" />
      </Button>
      <Button
        variant="ghost"
        size="icon"
        onClick={onRefresh}
        disabled={loading}
        title={t(I18N_KEYS.COMMON.REFRESH)}
        className="h-6 w-6"
      >
        <RefreshCcw className="w-3.5 h-3.5 theme-text-secondary" />
      </Button>
      <TransactionModeSelector
        transactionMode={txMode}
        isolationLevel={isolationLevel}
        onTransactionModeChange={onTransactionModeChange}
        onIsolationLevelChange={onIsolationLevelChange}
      />
      <div className="w-px h-4 bg-border mx-0.5" />
      <Button
        variant="ghost"
        size="icon"
        className="h-6 w-6"
        title={t(I18N_KEYS.EXPLORER.ADD_ROW)}
        onClick={onAddRow}
        disabled={!isTable || loading || isTransposeMode}
      >
        <Plus className="w-3.5 h-3.5 theme-text-secondary" />
      </Button>
      <Button
        variant="ghost"
        size="icon"
        className="h-6 w-6"
        title={t(I18N_KEYS.EXPLORER.DELETE_ROW)}
        onClick={onDeleteRow}
        disabled={!isTable || !hasRowSelection || deletePending || isTransposeMode}
      >
        <Minus className="w-3.5 h-3.5 theme-text-secondary" />
      </Button>
      <div className="w-px h-4 bg-border mx-0.5" />
      <Button
        variant="ghost"
        size="sm"
        className="h-6 px-2 gap-1 text-[11px]"
        onClick={onOpenDdl}
        title={t(I18N_KEYS.EXPLORER.VIEW_DDL)}
      >
        <FileText className="w-3.5 h-3.5 theme-text-secondary" />
        DDL
      </Button>
      <div className="w-px h-4 bg-border mx-0.5" />
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="h-6 px-2 rounded flex items-center gap-1 text-[10px] theme-text-primary hover:bg-accent/30 transition-colors">
            <span>{viewMode === 'grid' ? t(I18N_KEYS.EXPLORER.GRID_VIEW) : t(I18N_KEYS.EXPLORER.TRANSPOSE_VIEW)}</span>
            <ChevronDown className="w-3 h-3 theme-text-secondary" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start" className="min-w-[100px]">
          <DropdownMenuItem
            onClick={() => onViewModeChange('grid')}
            className={`text-[11px] px-2 py-1.5 ${
              viewMode === 'grid' ? 'theme-text-primary font-semibold' : 'theme-text-secondary'
            }`}
          >
            <span className="w-4">{viewMode === 'grid' && <span>✓</span>}</span>
            <span className="ml-2">{t(I18N_KEYS.EXPLORER.GRID_VIEW)}</span>
          </DropdownMenuItem>
          <DropdownMenuItem
            onClick={() => onViewModeChange('transpose')}
            className={`text-[11px] px-2 py-1.5 ${
              viewMode === 'transpose' ? 'theme-text-primary font-semibold' : 'theme-text-secondary'
            }`}
          >
            <span className="w-4">{viewMode === 'transpose' && <span>✓</span>}</span>
            <span className="ml-2">{t(I18N_KEYS.EXPLORER.TRANSPOSE_VIEW)}</span>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <div className="flex-1" />
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="h-6 px-2 rounded flex items-center gap-1 text-[11px] theme-text-primary hover:bg-accent/30 transition-colors">
            <span>{displayDbLabel || connectionName}</span>
            <ChevronDown className="w-3 h-3 theme-text-secondary" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="min-w-[140px]">
          {loadingDatabases ? (
            <div className="px-3 py-2 text-[10px] theme-text-secondary">
              {t(I18N_KEYS.COMMON.LOADING)}...
            </div>
          ) : databases.length === 0 ? (
            <div className="px-3 py-2 text-[10px] theme-text-secondary">
              {t(I18N_KEYS.COMMON.NO_DATA)}
            </div>
          ) : (
            databases.map((db) => (
              <DropdownMenuItem
                key={db}
                onClick={() => onDatabaseChange(db)}
                className={`text-[11px] px-2 py-1.5 ${
                  displayDbLabel === db
                    ? 'theme-text-primary font-semibold'
                    : 'theme-text-secondary'
                }`}
              >
                <span className="w-4">{displayDbLabel === db && <span>✓</span>}</span>
                <span className="ml-2">{db}</span>
              </DropdownMenuItem>
            ))
          )}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
