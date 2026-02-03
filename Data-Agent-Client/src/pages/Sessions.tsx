import { useTranslation } from 'react-i18next';
import { SessionManager } from '../components/common/SessionManager';

export default function Sessions() {
    const { t } = useTranslation();
    return (
        <div>
            <div className="mb-6">
                <h2 className="text-xl font-semibold">{t('sessions.page_title')}</h2>
                <p className="text-sm text-muted-foreground mt-1">
                    {t('sessions.page_desc')}
                </p>
            </div>
            <SessionManager />
        </div>
    );
}
