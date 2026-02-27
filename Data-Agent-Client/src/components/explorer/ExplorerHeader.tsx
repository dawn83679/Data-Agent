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
    <div>
      {/* Title Bar */}
      <div className="flex items-center justify-between px-3 py-2 theme-text-secondary text-[10px] uppercase font-bold tracking-wider border-b theme-border shrink-0">
        <span>{t(I18N_KEYS.EXPLORER.TITLE)}</span>
        <div className="flex items-center space-x-2">
          <button onClick={onRefresh} title={t(I18N_KEYS.COMMON.REFRESH)}>
            <RefreshCw className={cn('w-3 h-3 hover:text-blue-500 transition-colors', isLoading && 'animate-spin')} />
          </button>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button title={t(I18N_KEYS.COMMON.ADD)} aria-label={t(I18N_KEYS.COMMON.ADD)}>
                <Plus className="w-3.5 h-3.5 hover:text-blue-500 cursor-pointer" />
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

      {/* Search Bar */}
      <div className="p-2 shrink-0">
        <div className="relative">
          <Search className="absolute left-2 top-1/2 -translate-y-1/2 w-3 h-3 theme-text-secondary" />
          <input
            type="text"
            placeholder={t(I18N_KEYS.COMMON.SEARCH)}
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            className="w-full bg-accent/30 border theme-border rounded px-7 py-1 text-[11px] focus:outline-none focus:border-primary/50"
          />
        </div>
      </div>
    </div>
  );
}
