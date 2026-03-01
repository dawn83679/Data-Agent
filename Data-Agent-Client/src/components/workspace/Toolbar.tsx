import { Play, Square, CheckCircle, RotateCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { ReactNode } from 'react';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useEffect, useState } from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/DropdownMenu';
import { databaseService } from '../../services/database.service';
import { Button } from '../ui/Button';
import { TransactionMode, IsolationLevel } from '../../constants/transactionSettings';
import { TransactionModeSelector } from './TransactionModeSelector';

interface ToolbarProps {
  onRun: () => void;
  onStop?: () => void;
  isRunning?: boolean;
  connectionId?: number;
  currentDatabase?: string | null;
  onDatabaseChange?: (db: string) => void;
  extraActions?: ReactNode;
}

export function Toolbar({
  onRun,
  onStop,
  isRunning = false,
  connectionId,
  currentDatabase,
  onDatabaseChange,
  extraActions,
}: ToolbarProps) {
  const { t } = useTranslation();
  const [databases, setDatabases] = useState<string[]>([]);
  const [loadingDatabases, setLoadingDatabases] = useState(false);
  const [transactionMode, setTransactionMode] = useState<TransactionMode>(TransactionMode.AUTO);
  const [isolationLevel, setIsolationLevel] = useState<IsolationLevel>(IsolationLevel.DEFAULT);

  useEffect(() => {
    if (!connectionId) {
      setDatabases([]);
      return;
    }

    const loadDatabases = async () => {
      setLoadingDatabases(true);
      try {
        const dbs = await databaseService.listDatabases(String(connectionId));
        setDatabases(dbs || []);
      } catch (error) {
        console.error('Failed to load databases:', error);
      } finally {
        setLoadingDatabases(false);
      }
    };

    loadDatabases();
  }, [connectionId]);

  return (
    <>
      {/* Left: Action Buttons */}
      <Button
        variant="ghost"
        size="icon"
        onClick={onRun}
        disabled={!connectionId || isRunning}
        title={isRunning ? t(I18N_KEYS.COMMON.STOP) : t(I18N_KEYS.WORKSPACE.RUN_SHORTCUT)}
        className="h-6 w-6"
      >
        {isRunning ? (
          <Square className="w-3.5 h-3.5 fill-current text-amber-500" />
        ) : (
          <Play className="w-3.5 h-3.5 fill-current text-green-500" />
        )}
      </Button>

      {/* Divider */}
      <div className="w-px h-4 bg-border mx-0.5" />

      {/* Transaction Mode Selector */}
      <TransactionModeSelector
        transactionMode={transactionMode}
        isolationLevel={isolationLevel}
        onTransactionModeChange={setTransactionMode}
        onIsolationLevelChange={setIsolationLevel}
      />

      {extraActions && (
        <>
          {/* Divider */}
          <div className="w-px h-4 bg-border mx-0.5" />
          <div className="flex items-center gap-1">
            {extraActions}
          </div>
        </>
      )}

      {/* Commit and Rollback Buttons (Manual Mode Only) */}
      {transactionMode === TransactionMode.MANUAL && (
        <>
          <Button
            variant="ghost"
            size="icon"
            className="h-6 w-6"
            title={t(I18N_KEYS.WORKSPACE.COMMIT)}
          >
            <CheckCircle className="w-3.5 h-3.5 text-green-500 hover:text-green-600" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-6 w-6"
            title={t(I18N_KEYS.WORKSPACE.ROLLBACK)}
          >
            <RotateCcw className="w-3.5 h-3.5 text-orange-500 hover:text-orange-600" />
          </Button>
        </>
      )}

      {/* Divider */}
      <div className="w-px h-4 bg-border mx-0.5" />

      {/* Stop Button */}
      <Button
        variant="ghost"
        size="icon"
        onClick={onStop}
        disabled={!isRunning}
        title={t(I18N_KEYS.COMMON.STOP)}
        className="h-6 w-6"
      >
        <Square className={`w-3.5 h-3.5 fill-current ${isRunning ? 'text-red-500' : 'text-gray-400'}`} />
      </Button>

      {/* Flex spacer */}
      <div className="flex-1" />

      {/* Right: Database Selector */}
      {connectionId && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="h-6 px-2 rounded flex items-center gap-1 text-[11px] hover:bg-accent/30 transition-colors">
              <span className="theme-text-primary font-medium">{currentDatabase || 'db'}</span>
              <span className="text-[9px] theme-text-secondary">▾</span>
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
              <>
                {databases.map((db) => (
                  <DropdownMenuItem
                    key={db}
                    onClick={() => onDatabaseChange?.(db)}
                    className={`text-[11px] px-2 py-1.5 ${
                      currentDatabase === db
                        ? 'theme-text-primary font-semibold'
                        : 'theme-text-secondary'
                    }`}
                  >
                    <span className="w-4">
                      {currentDatabase === db && <span>✓</span>}
                    </span>
                    <span className="ml-2">{db}</span>
                  </DropdownMenuItem>
                ))}
              </>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
    </>
  );
}
