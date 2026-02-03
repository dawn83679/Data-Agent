import { ChevronRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface BreadcrumbsProps {
    activeTabName?: string;
}

export function Breadcrumbs({ activeTabName }: BreadcrumbsProps) {
    const { t } = useTranslation();
    return (
        <div className="flex items-center">
            <span className="hover:theme-text-primary cursor-pointer transition-colors">{t('common.data_sources')}</span>
            <ChevronRight className="w-3 h-3 mx-1 opacity-50" />
            <span className="hover:theme-text-primary cursor-pointer transition-colors">{t('common.default')}</span>
            {activeTabName && (
                <>
                    <ChevronRight className="w-3 h-3 mx-1 opacity-50" />
                    <span className="theme-text-primary font-medium">{activeTabName}</span>
                </>
            )}
        </div>
    );
}
