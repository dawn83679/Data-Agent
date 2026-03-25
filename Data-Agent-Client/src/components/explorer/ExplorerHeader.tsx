import { Plus, Search, RefreshCw, Settings, Database } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { cn } from '../../lib/utils';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSub,
  DropdownMenuSubTrigger,
  DropdownMenuSubContent,
  DropdownMenuPortal,
} from '../ui/DropdownMenu';

interface ExplorerHeaderProps {
  searchTerm: string;
  onSearchChange: (term: string) => void;
  isLoading: boolean;
  onRefresh: () => void;
  supportedDbTypes: Array<{ code: string; displayName: string }>;
  onAddDatabase: (dbType: string) => void;
  onManageDriver: (dbType: string) => void;
}

export function ExplorerHeader({
  searchTerm,
  onSearchChange,
  isLoading,
  onRefresh,
  supportedDbTypes,
  onAddDatabase,
  onManageDriver,
}: ExplorerHeaderProps) {
  const { t } = useTranslation();

  return (
    <div className="workbench-header shrink-0">
      {/* Search Block (first) */}
      <div className="border-b border-[color:var(--workbench-header-border)] px-3 py-3">
        <div className="workbench-search-shell relative flex items-center gap-2 rounded-xl p-1.5">
          <Search className="ml-1 h-3.5 w-3.5 theme-text-secondary" />
          <input
            type="text"
            placeholder={t(I18N_KEYS.COMMON.SEARCH)}
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            className="h-6 w-full bg-transparent border-none p-0 text-[12px] theme-text-primary placeholder:theme-text-secondary/80 focus:outline-none focus:ring-0"
          />
          <span className="mr-0.5 inline-flex items-center rounded-lg border border-[color:var(--workbench-chip-active-border)] bg-[color:var(--workbench-chip-active-bg)] px-1.5 py-0.5 text-[10px] font-semibold theme-text-primary shadow-sm">
            K
          </span>
        </div>
      </div>

      {/* Explorer Title + Actions (second) */}
      <div className="flex items-center justify-between px-3 py-2.5 text-[10px] font-semibold uppercase tracking-[0.14em] theme-text-secondary">
        <span>{t(I18N_KEYS.EXPLORER.TITLE)}</span>
        <div className="flex items-center gap-1.5">
          <button
            onClick={onRefresh}
            title={t(I18N_KEYS.COMMON.REFRESH)}
            className="workbench-icon-button"
          >
            <RefreshCw className={cn('h-3.5 w-3.5 hover:theme-text-primary transition-colors', isLoading && 'animate-spin')} />
          </button>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                title={t(I18N_KEYS.COMMON.ADD)}
                aria-label={t(I18N_KEYS.COMMON.ADD)}
                className="workbench-icon-button"
              >
                <Plus className="h-3.5 w-3.5 hover:theme-text-primary cursor-pointer transition-colors" />
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-48">
              <DropdownMenuSub>
                <DropdownMenuSubTrigger>
                  <Database className="w-4 h-4 mr-2 text-blue-400" />
                  <span>{t(I18N_KEYS.EXPLORER.DATABASE)}</span>
                </DropdownMenuSubTrigger>
                <DropdownMenuPortal>
                  <DropdownMenuSubContent className="w-48">
                    {supportedDbTypes.length === 0 ? (
                      <DropdownMenuItem disabled className="text-xs theme-text-secondary">
                        {t(I18N_KEYS.EXPLORER.LOADING)}
                      </DropdownMenuItem>
                    ) : (
                      supportedDbTypes.map((type) => (
                        <DropdownMenuItem key={type.code} onClick={() => onAddDatabase(type.code)}>
                          <Database className="w-4 h-4 mr-2 text-blue-400" />
                          <span>{type.displayName}</span>
                        </DropdownMenuItem>
                      ))
                    )}
                  </DropdownMenuSubContent>
                </DropdownMenuPortal>
              </DropdownMenuSub>

              <DropdownMenuSub>
                <DropdownMenuSubTrigger>
                  <Settings className="w-4 h-4 mr-2" />
                  <span>{t(I18N_KEYS.EXPLORER.DRIVER)}</span>
                </DropdownMenuSubTrigger>
                <DropdownMenuPortal>
                  <DropdownMenuSubContent className="w-48">
                    {supportedDbTypes.length === 0 ? (
                      <DropdownMenuItem disabled className="text-xs theme-text-secondary">
                        {t(I18N_KEYS.EXPLORER.LOADING)}
                      </DropdownMenuItem>
                    ) : (
                      supportedDbTypes.map((type) => (
                        <DropdownMenuItem key={type.code} onClick={() => onManageDriver(type.code)}>
                          <Database className="w-4 h-4 mr-2 text-blue-400" />
                          <span>{type.displayName}</span>
                        </DropdownMenuItem>
                      ))
                    )}
                  </DropdownMenuSubContent>
                </DropdownMenuPortal>
              </DropdownMenuSub>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </div>
  );
}
