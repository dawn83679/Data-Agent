import { Play, RotateCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface ToolbarProps {
    onRun: () => void;
    onRollback?: () => void;
}

export function Toolbar({ onRun, onRollback }: ToolbarProps) {
    const { t } = useTranslation();
    return (
        <div className="flex items-center space-x-3">
            <button 
                onClick={onRun}
                className="flex items-center space-x-1 text-green-500 hover:text-green-400 transition-colors"
                title={t('workspace.run_shortcut')}
            >
                <Play className="w-3.5 h-3.5 fill-current" />
                <span className="font-bold text-[10px]">{t('common.run')}</span>
            </button>
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
