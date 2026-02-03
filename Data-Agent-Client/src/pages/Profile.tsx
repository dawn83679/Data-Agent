import { useTranslation } from 'react-i18next';
import { UserProfileEditor } from '../components/common/UserProfileEditor';

export default function Profile() {
    const { t } = useTranslation();
    return (
        <div>
            <div className="mb-6">
                <h2 className="text-xl font-semibold">{t('profile.page_title')}</h2>
                <p className="text-sm text-muted-foreground mt-1">
                    {t('profile.page_desc')}
                </p>
            </div>
            <UserProfileEditor />
        </div>
    );
}
