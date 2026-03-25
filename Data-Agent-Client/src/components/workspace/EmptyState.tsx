import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { getPlatformShortcuts } from '../../lib/platformShortcuts';

export function EmptyState() {
    const { t } = useTranslation();
    const shortcuts = getPlatformShortcuts();

    const shortcutItems = [
        { label: t(I18N_KEYS.COMMON.EXECUTE_QUERY), keys: shortcuts.runQuery },
        { label: t(I18N_KEYS.COMMON.INSERT_INDENT), keys: 'Tab' },
        { label: t(I18N_KEYS.COMMON.OPEN_SETTINGS), keys: shortcuts.openSettings },
        { label: t(I18N_KEYS.COMMON.CLOSE_EXPLORER), keys: 'Esc' },
        { label: t(I18N_KEYS.COMMON.TOGGLE_AI), keys: shortcuts.toggleAI },
    ];

    const shortcutCardStyle = {
        background: 'linear-gradient(180deg, var(--workbench-header-bg-top), var(--workbench-header-bg-bottom))',
        boxShadow: 'var(--workbench-chip-active-shadow)',
    } as const;

    return (
        <div className="flex-1 h-full flex flex-col overflow-hidden relative bg-transparent select-none">
            <div className="flex-1 flex items-center justify-center p-8 overflow-y-auto">
                <div
                    className="max-w-xl w-full rounded-[24px] border border-[color:var(--workbench-chip-hover-border)] px-6 py-6 backdrop-blur-xl"
                    style={shortcutCardStyle}
                >
                    <div className="mb-2 text-[10px] font-semibold uppercase tracking-[0.18em] theme-text-secondary">
                        Workspace
                    </div>
                    <h2 className="mb-6 text-xl font-medium theme-text-primary">{t(I18N_KEYS.COMMON.KEYBOARD_SHORTCUTS)}</h2>
                    <div className="space-y-3 text-sm">
                        {shortcutItems.map((s, i) => (
                            <div
                                key={i}
                                className="flex items-center justify-between gap-8 rounded-xl border border-transparent px-3 py-2.5 transition-colors hover:border-[color:var(--workbench-chip-hover-border)] hover:bg-[color:var(--workbench-chip-hover-bg)]"
                            >
                                <span className="theme-text-primary">{s.label}</span>
                                <span className="workbench-pill font-mono text-[11px] text-[var(--accent-blue-subtle)]">
                                    {s.keys}
                                </span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
            <div className="absolute bottom-4 left-0 w-full text-center text-xs uppercase tracking-[0.14em] theme-text-secondary pointer-events-none">
                {t(I18N_KEYS.COMMON.DRAG_HINT)}
            </div>
        </div>
    );
}
