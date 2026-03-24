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
        <div className="flex-1 h-full flex flex-col overflow-hidden relative theme-bg-panel select-none">
            <div className="flex-1 flex items-center justify-center p-8 overflow-y-auto">
                <div className="max-w-md w-full">
                    <h2 className="text-xl font-medium mb-6 theme-text-primary">{t(I18N_KEYS.COMMON.KEYBOARD_SHORTCUTS)}</h2>
                    <div className="space-y-4 text-sm">
                        {shortcuts.map((s, i) => (
                            <div key={i} className="flex justify-between items-center gap-8">
                                <span className="theme-text-primary">{s.label}</span>
                                <span className="font-mono text-sm text-[var(--accent-blue-subtle)]">{s.keys}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
            <div className="absolute bottom-4 left-0 w-full text-center text-sm theme-text-secondary pointer-events-none">
                {t(I18N_KEYS.COMMON.DRAG_HINT)}
            </div>
        </div>
    );
}
