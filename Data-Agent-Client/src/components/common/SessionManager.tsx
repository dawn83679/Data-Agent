import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { sessionService } from '../../services/session.service';
import { SessionInfo } from '../../types/auth';
import { Button } from '../ui/Button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../ui/Dialog';
import { Monitor, Smartphone, Trash2, RefreshCw } from 'lucide-react';
import { useToast } from '../../hooks/useToast';
import { resolveErrorMessage } from '../../lib/errorMessage';

export function SessionManager() {
    const { t } = useTranslation();
    const [sessions, setSessions] = useState<SessionInfo[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [sessionToRevoke, setSessionToRevoke] = useState<number | null>(null);
    const toast = useToast();

    const loadSessions = async () => {
        setIsLoading(true);
        setError(null);
        try {
            const data = await sessionService.listActiveSessions();
            setSessions(data);
        } catch (err: any) {
            setError(resolveErrorMessage(err, t('sessions.load_failed')));
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        loadSessions();
    }, []);

    const handleRevokeSession = async (sessionId: number) => {
        setSessionToRevoke(sessionId);
    };

    const confirmRevoke = async () => {
        if (!sessionToRevoke) return;

        try {
            await sessionService.revokeSession(sessionToRevoke);
            setSessions(sessions.filter((s) => s.id !== sessionToRevoke));
            toast.success(t('sessions.revoke_success'));
        } catch (err: any) {
            toast.error(resolveErrorMessage(err, t('sessions.revoke_failed')));
        } finally {
            setSessionToRevoke(null);
        }
    };

    const formatDate = (dateStr: string) => {
        return new Date(dateStr).toLocaleString();
    };

    const getDeviceIcon = (userAgent?: string) => {
        if (!userAgent) return <Monitor className="h-4 w-4" />;
        const ua = userAgent.toLowerCase();
        if (ua.includes('mobile') || ua.includes('android') || ua.includes('iphone')) {
            return <Smartphone className="h-4 w-4" />;
        }
        return <Monitor className="h-4 w-4" />;
    };

    const getDeviceName = (userAgent?: string): string => {
        if (!userAgent) return t('sessions.unknown_device');
        const ua = userAgent.toLowerCase();

        // Browser detection
        let browser = 'Unknown Browser';
        if (ua.includes('chrome') && !ua.includes('edg')) browser = 'Chrome';
        else if (ua.includes('firefox')) browser = 'Firefox';
        else if (ua.includes('safari') && !ua.includes('chrome')) browser = 'Safari';
        else if (ua.includes('edg')) browser = 'Edge';
        else if (ua.includes('opera') || ua.includes('opr')) browser = 'Opera';

        // OS detection
        let os = 'Unknown OS';
        if (ua.includes('windows')) os = 'Windows';
        else if (ua.includes('mac os')) os = 'macOS';
        else if (ua.includes('linux')) os = 'Linux';
        else if (ua.includes('android')) os = 'Android';
        else if (ua.includes('iphone') || ua.includes('ipad')) os = 'iOS';

        return `${browser} on ${os}`;
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold">{t('sessions.list_title')}</h2>
                <Button variant="outline" size="sm" onClick={loadSessions} disabled={isLoading}>
                    <RefreshCw className={`h-4 w-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
                    {t('sessions.refresh')}
                </Button>
            </div>

            {error && (
                <div className="p-3 rounded-md text-sm bg-red-50 dark:bg-red-900/20 text-red-800 dark:text-red-200 border border-red-200 dark:border-red-800">
                    {error}
                </div>
            )}

            {isLoading && sessions.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">{t('sessions.loading')}</div>
            ) : sessions.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">{t('sessions.no_sessions')}</div>
            ) : (
                <div className="space-y-3">
                    {sessions.map((session) => (
                        <div
                            key={session.id}
                            className={`p-4 rounded-lg border ${session.isCurrent
                                    ? 'border-primary bg-primary/5'
                                    : 'border-border bg-card'
                                }`}
                        >
                            <div className="flex items-start justify-between gap-4">
                                <div className="flex items-start gap-3 flex-1">
                                    <div className="mt-1 text-muted-foreground">
                                        {getDeviceIcon(session.userAgent)}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2">
                                            <p className="font-medium text-sm">
                                                {getDeviceName(session.userAgent)}
                                            </p>
                                            {session.isCurrent && (
                                                <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-primary text-primary-foreground">
                                                    {t('sessions.current')}
                                                </span>
                                            )}
                                        </div>
                                        <p className="text-xs text-muted-foreground mt-1">
                                            {t('sessions.ip')}: {session.ipAddress || t('sessions.unknown')}
                                        </p>
                                        <p className="text-xs text-muted-foreground">
                                            {t('sessions.last_active')}: {formatDate(session.lastRefreshAt)}
                                        </p>
                                        <p className="text-xs text-muted-foreground">
                                            {t('sessions.logged_in')}: {formatDate(session.createdAt)}
                                        </p>
                                    </div>
                                </div>
                                {!session.isCurrent && (
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => handleRevokeSession(session.id)}
                                        className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                    >
                                        <Trash2 className="h-4 w-4" />
                                    </Button>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Revoke Session Confirmation Dialog */}
            <Dialog open={!!sessionToRevoke} onOpenChange={() => setSessionToRevoke(null)}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{t('sessions.dialog_title')}</DialogTitle>
                        <DialogDescription>
                            {t('sessions.dialog_desc')}
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setSessionToRevoke(null)}>
                            {t('sessions.cancel')}
                        </Button>
                        <Button variant="destructive" onClick={confirmRevoke}>
                            {t('sessions.logout')}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </div>
    );
}
