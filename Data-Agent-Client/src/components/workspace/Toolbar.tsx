import { Play, RotateCcw, AlignLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface ToolbarProps {
    onRun: () => void;
    onFormat?: () => void;
    onRollback?: () => void;
    isRunning?: boolean;
}

export function Toolbar({ onRun, onFormat, onRollback, isRunning }: ToolbarProps) {
    const { t } = useTranslation();
    return (
        <div className="flex items-center space-x-3">
            <button
                onClick={onRun}
                disabled={isRunning}
                className="flex items-center space-x-1 text-green-500 hover:text-green-400 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                title={t('workspace.run_shortcut')}
            >
                <Play className="w-3.5 h-3.5 fill-current" />
                <span className="font-bold text-[10px]">{t('common.run')}</span>
            </button>
            {onFormat && (
                <>
                    <div className="w-px h-3 bg-border" />
                    <button
                        onClick={onFormat}
                        className="flex items-center space-x-1 hover:theme-text-primary transition-colors text-[10px]"
                        title={t('common.format_sql')}
                    >
                        <AlignLeft className="w-3 h-3" />
                        <span>{t('common.format_sql')}</span>
                    </button>
                </>
            )}
            <div className="w-px h-3 bg-border" />
            <button
                onClick={onRollback}
                className="flex items-center space-x-1 hover:theme-text-primary transition-colors text-[10px]"
            >
                <RotateCcw className="w-3 h-3" />
                <span>{t('common.rollback')}</span>
            </button>
        </div>
    );
}
