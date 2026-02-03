import { FileCode, Table as TableIcon, X, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../lib/utils';
import { useWorkspaceStore } from '../../store/workspaceStore';

export function TabBar() {
  const { t } = useTranslation();
  const { tabs, switchTab, closeTab, openTab } = useWorkspaceStore();

  const handleAddTab = () => {
    const id = `console-${Date.now()}`;
    openTab({
      id,
      name: 'new_console.sql',
      type: 'file',
      content: ''
    });
  };

    return (
    <div className={cn(
      "h-9 theme-bg-panel flex items-end space-x-1 overflow-x-auto no-scrollbar border-b theme-border shrink-0",
      tabs.length === 0 && "hidden"
    )}>
      {tabs.map((tab) => (
        <div
          key={tab.id}
          onClick={() => switchTab(tab.id)}
          className={cn(
            "flex items-center px-3 py-1.5 text-[11px] rounded-t min-w-[120px] max-w-[200px] group select-none cursor-pointer border-t-2 transition-colors relative",
            tab.active 
              ? "tab-active border-primary" 
              : "theme-bg-panel theme-text-secondary hover:bg-accent/50 border-transparent"
          )}
        >
          <span className="mr-2 shrink-0">
            {tab.type === 'file' ? (
              <FileCode className="w-3 h-3 text-blue-400" />
            ) : (
              <TableIcon className="w-3 h-3 text-green-400" />
            )}
          </span>
          <span className="flex-1 truncate mr-4" title={tab.name}>
            {tab.name}
          </span>
          <button
            onClick={(e) => {
              e.stopPropagation();
              closeTab(tab.id);
            }}
            className="absolute right-1.5 p-0.5 rounded opacity-0 group-hover:opacity-100 hover:bg-accent/80 hover:text-red-400 transition-all"
          >
            <X className="w-2.5 h-2.5" />
          </button>
        </div>
      ))}
      <button 
        onClick={handleAddTab}
        className="flex items-center justify-center w-8 h-8 mb-0.5 theme-text-secondary hover:text-blue-500 transition-colors"
        title={t('workspace.new_console_tab_title')}
      >
        <Plus className="w-3.5 h-3.5" />
      </button>
    </div>
  );
}
