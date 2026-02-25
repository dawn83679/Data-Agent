import { useTranslation } from 'react-i18next';
import { SessionManager } from '../components/common/SessionManager';
import { I18N_KEYS } from '../constants/i18nKeys';

export default function Sessions() {
    const { t } = useTranslation();
    return (
        <div>
            <div className="mb-6">
                <h2 className="text-xl font-semibold">{t(I18N_KEYS.SESSIONS.PAGE_TITLE)}</h2>
                <p className="text-sm text-muted-foreground mt-1">
                    {t(I18N_KEYS.SESSIONS.PAGE_DESC)}
                </p>
            </div>
            <SessionManager />
        </div>
    );
}
