import { useTranslation } from 'react-i18next';
import { UserProfileEditor } from '../components/common/UserProfileEditor';
import { I18N_KEYS } from '../constants/i18nKeys';

export default function Profile() {
    const { t } = useTranslation();
    return (
        <div>
            <div className="mb-6">
                <h2 className="text-xl font-semibold">{t(I18N_KEYS.PROFILE.PAGE_TITLE)}</h2>
                <p className="text-sm text-muted-foreground mt-1">
                    {t(I18N_KEYS.PROFILE.PAGE_DESC)}
                </p>
            </div>
            <UserProfileEditor />
        </div>
    );
}
