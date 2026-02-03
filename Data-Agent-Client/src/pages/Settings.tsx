import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { User, Shield, Lock, ChevronRight, ArrowLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '../lib/utils';
import { Button } from '../components/ui/Button';

const navItems = [
    { path: '/settings/profile', labelKey: 'settingsPage.nav_profile' as const, icon: User },
    { path: '/settings/password', labelKey: 'settingsPage.nav_security' as const, icon: Lock },
    { path: '/settings/sessions', labelKey: 'settingsPage.nav_sessions' as const, icon: Shield },
] as const;

export default function Settings() {
    const { t } = useTranslation();
    const location = useLocation();
    const navigate = useNavigate();

    return (
        <div className="max-w-5xl mx-auto">
            <div className="mb-8 flex items-start justify-between">
                <div>
                    <div className="flex items-center gap-2 mb-2">
                        <Button 
                            variant="ghost" 
                            size="sm" 
                            onClick={() => navigate('/')}
                            className="h-8 px-2 -ml-2 theme-text-secondary hover:theme-text-primary"
                        >
                            <ArrowLeft className="h-4 w-4 mr-1" />
                            {t('settingsPage.back_to_workspace')}
                        </Button>
                    </div>
                    <h1 className="text-3xl font-bold">{t('settingsPage.title')}</h1>
                    <p className="text-muted-foreground mt-1">
                        {t('settingsPage.subtitle')}
                    </p>
                </div>
            </div>

            <div className="flex flex-col md:flex-row gap-8">
                <nav className="w-full md:w-64 flex-shrink-0">
                    <div className="bg-card rounded-lg border border-border overflow-hidden">
                        <div className="p-4 border-b border-border bg-muted/30">
                            <h2 className="font-medium text-sm">{t('settingsPage.account')}</h2>
                        </div>
                        <ul className="p-2 space-y-1">
                            {navItems.map((item) => {
                                const Icon = item.icon;
                                const isActive = location.pathname === item.path;

                                return (
                                    <li key={item.path}>
                                        <NavLink
                                            to={item.path}
                                            className={cn(
                                                'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
                                                isActive
                                                    ? 'bg-primary/10 text-primary font-medium'
                                                    : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                                            )}
                                        >
                                            <Icon className="h-4 w-4" />
                                            {t(item.labelKey)}
                                            {isActive && (
                                                <ChevronRight className="h-3 w-3 ml-auto" />
                                            )}
                                        </NavLink>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                </nav>

                {/* Main Content Area */}
                <main className="flex-1 min-w-0">
                    <div className="bg-card rounded-lg border border-border p-6">
                        <Outlet />
                    </div>
                </main>
            </div>
        </div>
    );
}
