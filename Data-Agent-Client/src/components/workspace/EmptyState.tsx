import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';

export function EmptyState() {
    const { t } = useTranslation();
    
    const shortcuts = [
        { label: t(I18N_KEYS.COMMON.EXECUTE_QUERY), keys: 'Ctrl+Enter / Cmd+Enter' },
        { label: t(I18N_KEYS.COMMON.INSERT_INDENT), keys: 'Tab' },
        { label: t(I18N_KEYS.COMMON.OPEN_SETTINGS), keys: 'Ctrl+Shift+, / Cmd+Shift+,' },
        { label: t(I18N_KEYS.COMMON.CLOSE_EXPLORER), keys: 'Esc' },
        { label: t(I18N_KEYS.COMMON.TOGGLE_AI), keys: 'Ctrl+B / Cmd+B' },
    ];

    return (
        <div className="flex-1 h-full flex flex-col items-center justify-center theme-bg-main select-none">
            <div className="text-base theme-text-secondary space-y-3 text-left">
                <div className="space-y-2">
                    {shortcuts.map((s, i) => (
                        <div key={i} className="flex gap-6">
                            <span className="theme-text-primary w-56">{s.label}</span>
                            <span className="font-mono text-sm">{s.keys}</span>
                        </div>
                    ))}
                </div>
                <div className="mt-4 text-sm">
                    {t(I18N_KEYS.COMMON.DRAG_HINT)}
                </div>
            </div>
        </div>
    );
}
