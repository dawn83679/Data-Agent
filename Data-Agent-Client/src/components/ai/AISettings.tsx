import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useWorkspaceStore } from '../../store/workspaceStore';
import { cn } from '../../lib/utils';

interface AISettingsProps {
  onClose: () => void;
}

export function AISettings({ onClose }: AISettingsProps) {
  const { t } = useTranslation();
  const { 
    aiAutoSelect, 
    aiAutoWrite, 
    aiWriteTransaction, 
    aiMaxRetries, 
    updatePreferences 
  } = useWorkspaceStore();

  return (
    <div className="absolute right-0 top-6 w-64 theme-bg-panel rounded-lg shadow-xl border theme-border z-50 animate-in fade-in slide-in-from-top-1 duration-200">
      <div className="p-3 space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-[11px] uppercase font-bold theme-text-secondary tracking-wider">{t('ai.settings')}</span>
          <X className="w-3 h-3 cursor-pointer theme-text-secondary hover:theme-text-primary" onClick={onClose} />
        </div>
        
        <div className="space-y-2.5 pt-1">
          <label className="flex items-center justify-between cursor-pointer group">
            <span className="text-[11px] theme-text-secondary group-hover:theme-text-primary transition-colors">{t('ai.auto_select')}</span>
            <input 
              type="checkbox" 
              checked={aiAutoSelect}
              onChange={(e) => updatePreferences({ aiAutoSelect: e.target.checked })}
              className="form-checkbox h-3 w-3 rounded text-blue-500 bg-transparent border-theme-border focus:ring-0"
            />
          </label>
          
          <label className="flex items-center justify-between cursor-pointer group">
            <span className="text-[11px] theme-text-secondary group-hover:theme-text-primary transition-colors">{t('ai.auto_write')}</span>
            <input 
              type="checkbox" 
              checked={aiAutoWrite}
              onChange={(e) => updatePreferences({ aiAutoWrite: e.target.checked })}
              className="form-checkbox h-3 w-3 rounded text-blue-500 bg-transparent border-theme-border focus:ring-0"
            />
          </label>

          <div className={cn("ml-3 space-y-2 transition-opacity", !aiAutoWrite && "opacity-50 pointer-events-none")}>
            <label className="flex items-center justify-between cursor-pointer group">
              <span className="text-[10px] theme-text-secondary group-hover:theme-text-primary transition-colors">{t('ai.transaction')}</span>
              <input 
                type="checkbox" 
                checked={aiWriteTransaction}
                onChange={(e) => updatePreferences({ aiWriteTransaction: e.target.checked })}
                className="form-checkbox h-2.5 w-2.5 rounded text-blue-500 bg-transparent border-theme-border focus:ring-0"
              />
            </label>
          </div>

          <div className="pt-1 flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
              <span className="text-[11px] theme-text-secondary">{t('ai.max_retries')}</span>
              <input 
                type="number" 
                min={1} 
                max={10}
                value={aiMaxRetries}
                onChange={(e) => updatePreferences({ aiMaxRetries: parseInt(e.target.value) })}
                className="w-12 h-6 px-1.5 theme-bg-main border theme-border rounded text-[11px] theme-text-primary focus:border-blue-500 outline-none"
              />
            </div>
            <p className="text-[9px] theme-text-secondary opacity-60 italic">{t('ai.retries_hint')}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
