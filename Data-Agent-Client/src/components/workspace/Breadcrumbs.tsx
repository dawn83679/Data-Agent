import { ChevronRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';

interface BreadcrumbsProps {
    activeTabName?: string;
}

export function Breadcrumbs({ activeTabName }: BreadcrumbsProps) {
    const { t } = useTranslation();
    return (
        <div className="flex items-center">
            <span className="hover:theme-text-primary cursor-pointer transition-colors">{t(I18N_KEYS.COMMON.DATA_SOURCES)}</span>
            <ChevronRight className="w-3 h-3 mx-1 opacity-50" />
            <span className="hover:theme-text-primary cursor-pointer transition-colors">{t(I18N_KEYS.COMMON.DEFAULT)}</span>
            {activeTabName && (
                <>
                    <ChevronRight className="w-3 h-3 mx-1 opacity-50" />
                    <span className="theme-text-primary font-medium">{activeTabName}</span>
                </>
            )}
        </div>
    );
}
