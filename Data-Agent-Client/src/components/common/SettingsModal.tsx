import { X, RotateCcw } from 'lucide-react';
import { useWorkspaceStore } from '../../store/workspaceStore';
import { useTheme } from '../../hooks/useTheme';
import { cn } from '../../lib/utils';
import { useToast } from '../../hooks/useToast';
import { useTranslation } from 'react-i18next';
import i18n from '../../i18n';

export function SettingsModal() {
    const { 
        isSettingsModalOpen, 
        setSettingsModalOpen,
        resultBehavior,
        tableDblClickMode,
        tableDblClickConsoleTarget,
        updatePreferences,
        resetPreferences
    } = useWorkspaceStore();
    
    const { theme, setTheme } = useTheme();
    const toast = useToast();
    const { t } = useTranslation();
    const currentLang = i18n.language?.startsWith('zh') ? 'zh' : 'en';

    const handleLanguageChange = (code: string) => {
        i18n.changeLanguage(code);
        localStorage.setItem('i18nextLng', code);
    };

    if (!isSettingsModalOpen) return null;

    const handleReset = () => {
        if (window.confirm(t('settings.reset_confirm'))) {
            resetPreferences();
            toast.success(t('settings.preferences_reset'));
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 z-[200] flex items-center justify-center font-sans animate-in fade-in duration-200">
            <div className="theme-bg-popup w-[450px] rounded-lg shadow-2xl border theme-border flex flex-col text-sm animate-in zoom-in-95 duration-200">
                {/* Header */}
                <div className="px-4 py-3 border-b theme-border font-semibold theme-text-primary flex justify-between items-center select-none">
                    <span className="text-sm">{t('common.settings')}</span>
                    <button 
                        onClick={() => setSettingsModalOpen(false)}
                        className="theme-text-secondary hover:theme-text-primary transition-colors"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>

                {/* Body */}
                <div className="p-5 space-y-6 overflow-y-auto max-h-[70vh] no-scrollbar">
                    {/* Language */}
                    <section className="space-y-3">
                        <label className="block theme-text-secondary text-[11px] uppercase font-bold tracking-wider">{t('common.language')}</label>
                        <div className="grid grid-cols-2 gap-3">
                            <button
                                onClick={() => handleLanguageChange('zh')}
                                className={cn(
                                    "flex items-center justify-center gap-2 px-3 py-2 rounded border transition-all",
                                    currentLang === 'zh'
                                        ? "theme-bg-panel border-blue-500 text-blue-500"
                                        : "theme-bg-main theme-border theme-text-secondary hover:theme-bg-panel"
                                )}
                            >
                                <span>{t('settings.lang_zh')}</span>
                            </button>
                            <button
                                onClick={() => handleLanguageChange('en')}
                                className={cn(
                                    "flex items-center justify-center gap-2 px-3 py-2 rounded border transition-all",
                                    currentLang === 'en'
                                        ? "theme-bg-panel border-blue-500 text-blue-500"
                                        : "theme-bg-main theme-border theme-text-secondary hover:theme-bg-panel"
                                )}
                            >
                                <span>{t('settings.lang_en')}</span>
                            </button>
                        </div>
                    </section>

                    {/* Appearance */}
                    <section className="space-y-3 pt-4 border-t theme-border">
                        <label className="block theme-text-secondary text-[11px] uppercase font-bold tracking-wider">{t('settings.appearance')}</label>
                        <div className="grid grid-cols-2 gap-3">
                            <button
                                onClick={() => setTheme('dark')}
                                className={cn(
                                    "flex items-center justify-center gap-2 px-3 py-2 rounded border transition-all",
                                    theme === 'dark' 
                                        ? "theme-bg-panel border-blue-500 text-blue-500" 
                                        : "theme-bg-main theme-border theme-text-secondary hover:theme-bg-panel"
                                )}
                            >
                                <div className="w-3 h-3 rounded-full bg-slate-800 border border-slate-700" />
                                <span>{t('settings.dark')}</span>
                            </button>
                            <button
                                onClick={() => setTheme('light')}
                                className={cn(
                                    "flex items-center justify-center gap-2 px-3 py-2 rounded border transition-all",
                                    theme === 'light' 
                                        ? "theme-bg-panel border-blue-500 text-blue-500" 
                                        : "theme-bg-main theme-border theme-text-secondary hover:theme-bg-panel"
                                )}
                            >
                                <div className="w-3 h-3 rounded-full bg-slate-100 border border-slate-300" />
                                <span>{t('settings.light')}</span>
                            </button>
                        </div>
                    </section>

                    {/* Query Behavior */}
                    <section className="space-y-3 pt-4 border-t theme-border">
                        <label className="block theme-text-secondary text-[11px] uppercase font-bold tracking-wider">{t('settings.query_results')}</label>
                        
                        <div className="space-y-4">
                            <div className="space-y-2">
                                <span className="text-xs theme-text-primary font-medium">{t('settings.result_tabs_behavior')}</span>
                                <div className="space-y-2">
                                    <label className="flex items-center gap-2 cursor-pointer group">
                                        <input 
                                            type="radio" 
                                            name="result-behavior" 
                                            checked={resultBehavior === 'multi'}
                                            onChange={() => updatePreferences({ resultBehavior: 'multi' })}
                                            className="form-radio h-3.5 w-3.5 text-blue-500 bg-transparent border-theme-border focus:ring-0"
                                        />
                                        <span className="text-xs theme-text-secondary group-hover:theme-text-primary transition-colors">{t('settings.result_multi')}</span>
                                    </label>
                                    <label className="flex items-center gap-2 cursor-pointer group">
                                        <input 
                                            type="radio" 
                                            name="result-behavior" 
                                            checked={resultBehavior === 'overwrite'}
                                            onChange={() => updatePreferences({ resultBehavior: 'overwrite' })}
                                            className="form-radio h-3.5 w-3.5 text-blue-500 bg-transparent border-theme-border focus:ring-0"
                                        />
                                        <span className="text-xs theme-text-secondary group-hover:theme-text-primary transition-colors">{t('settings.result_overwrite')}</span>
                                    </label>
                                </div>
                            </div>

                            <div className="space-y-2">
                                <span className="text-xs theme-text-primary font-medium">{t('settings.table_dblclick')}</span>
                                <div className="space-y-2">
                                    <label className="flex items-center gap-2 cursor-pointer group">
                                        <input 
                                            type="radio" 
                                            name="table-dblclick" 
                                            checked={tableDblClickMode === 'table'}
                                            onChange={() => updatePreferences({ tableDblClickMode: 'table' })}
                                            className="form-radio h-3.5 w-3.5 text-blue-500 bg-transparent border-theme-border focus:ring-0"
                                        />
                                        <span className="text-xs theme-text-secondary group-hover:theme-text-primary transition-colors">{t('settings.table_dblclick_table')}</span>
                                    </label>
                                    <label className="flex items-center gap-2 cursor-pointer group">
                                        <input 
                                            type="radio" 
                                            name="table-dblclick" 
                                            checked={tableDblClickMode === 'console'}
                                            onChange={() => updatePreferences({ tableDblClickMode: 'console' })}
                                            className="form-radio h-3.5 w-3.5 text-blue-500 bg-transparent border-theme-border focus:ring-0"
                                        />
                                        <span className="text-xs theme-text-secondary group-hover:theme-text-primary transition-colors">{t('settings.table_dblclick_console')}</span>
                                    </label>
                                </div>
                                
                                {tableDblClickMode === 'console' && (
                                    <div className="ml-6 mt-2 p-2 rounded theme-bg-panel border theme-border space-y-2 animate-in slide-in-from-top-1 duration-200">
                                        <span className="text-[10px] theme-text-secondary uppercase font-bold">{t('settings.console_target')}</span>
                                        <div className="space-y-1.5">
                                            <label className="flex items-center gap-2 cursor-pointer group">
                                                <input 
                                                    type="radio" 
                                                    name="console-target" 
                                                    checked={tableDblClickConsoleTarget === 'reuse'}
                                                    onChange={() => updatePreferences({ tableDblClickConsoleTarget: 'reuse' })}
                                                    className="form-radio h-3 w-3 text-blue-500 bg-transparent border-theme-border focus:ring-0"
                                                />
                                                <span className="text-[11px] theme-text-secondary group-hover:theme-text-primary transition-colors">{t('settings.console_reuse')}</span>
                                            </label>
                                            <label className="flex items-center gap-2 cursor-pointer group">
                                                <input 
                                                    type="radio" 
                                                    name="console-target" 
                                                    checked={tableDblClickConsoleTarget === 'new'}
                                                    onChange={() => updatePreferences({ tableDblClickConsoleTarget: 'new' })}
                                                    className="form-radio h-3 w-3 text-blue-500 bg-transparent border-theme-border focus:ring-0"
                                                />
                                                <span className="text-[11px] theme-text-secondary group-hover:theme-text-primary transition-colors">{t('settings.console_new')}</span>
                                            </label>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </section>

                    {/* Reset */}
                    <div className="pt-4 border-t theme-border">
                        <button 
                            onClick={handleReset}
                            className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs rounded theme-bg-panel theme-text-secondary hover:theme-text-red-400 transition-all border theme-border hover:border-red-400/50"
                        >
                            <RotateCcw className="h-3 w-3" />
                            <span>{t('settings.reset_all')}</span>
                        </button>
                    </div>
                </div>

                {/* Footer */}
                <div className="px-4 py-3 border-t theme-border flex justify-end items-center theme-bg-panel rounded-b-lg select-none">
                    <button
                        onClick={() => setSettingsModalOpen(false)}
                        className="px-4 py-1.5 bg-blue-600 text-white hover:bg-blue-500 rounded transition-colors font-medium text-xs shadow-sm"
                    >
                        {t('settings.done')}
                    </button>
                </div>
            </div>
        </div>
    );
}
