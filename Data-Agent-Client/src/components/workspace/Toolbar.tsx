import { Play, Square, AlignLeft, RotateCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useEffect, useState } from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/DropdownMenu';
import { databaseService } from '../../services/database.service';
import { Button } from '../ui/Button';

interface ToolbarProps {
  onRun: () => void;
  onFormat?: () => void;
  isRunning?: boolean;
  onRollback?: () => void;
  connectionId?: number;
  currentDatabase?: string | null;
  onDatabaseChange?: (db: string) => void;
}

export function Toolbar({
  onRun,
  onFormat,
  isRunning = false,
  onRollback,
  connectionId,
  currentDatabase,
  onDatabaseChange,
}: ToolbarProps) {
  const { t } = useTranslation();
  const [databases, setDatabases] = useState<string[]>([]);
  const [loadingDatabases, setLoadingDatabases] = useState(false);

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
        title={isRunning ? t('common.stop') : t('workspace.run_shortcut')}
        className="h-6 w-6"
      >
        {isRunning ? (
          <Square className="w-3.5 h-3.5 fill-current text-amber-500" />
        ) : (
          <Play className="w-3.5 h-3.5 fill-current text-green-500" />
        )}
      </Button>

      {onFormat && (
        <Button
          variant="ghost"
          size="icon"
          onClick={onFormat}
          title={t('common.format_sql')}
          className="h-6 w-6"
        >
          <AlignLeft className="w-3 h-3" />
        </Button>
      )}

      <Button
        variant="ghost"
        size="icon"
        onClick={onRollback}
        title={t('common.rollback')}
        className="h-6 w-6"
      >
        <RotateCcw className="w-3 h-3" />
      </Button>

      {/* Divider */}
      <div className="w-px h-4 bg-border mx-0.5" />

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
                {t('common.loading')}...
              </div>
            ) : databases.length === 0 ? (
              <div className="px-3 py-2 text-[10px] theme-text-secondary">
                {t('common.no_data')}
              </div>
            ) : (
              <>
                {databases.map((db) => (
                  <DropdownMenuItem key={db} asChild>
                    <button
                      onClick={() => onDatabaseChange?.(db)}
                      className={`w-full px-3 py-1.5 text-[11px] text-left hover:bg-accent/30 transition-colors ${
                        currentDatabase === db
                          ? 'theme-text-primary font-semibold'
                          : 'theme-text-secondary'
                      }`}
                    >
                      {currentDatabase === db && <span className="mr-2">✓</span>}
                      {db}
                    </button>
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
