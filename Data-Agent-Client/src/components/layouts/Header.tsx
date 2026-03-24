import { useNavigate } from "react-router-dom";
import { Button } from "../ui/Button";
import { useAuthStore } from "../../store/authStore";
import { useWorkspaceStore } from "../../store/workspaceStore";
import { authService } from "../../services/auth.service";
import { LogOut, Settings, Wand2 } from "lucide-react";
import { resolveErrorMessage } from "../../lib/errorMessage";
import { useToast } from "../../hooks/useToast";
import { useTranslation } from "react-i18next";
import { I18N_KEYS } from "../../constants/i18nKeys";

interface HeaderProps {
    onLoginClick: () => void;
    onToggleAI?: () => void;
}

export function Header({ onLoginClick, onToggleAI }: HeaderProps) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { user, accessToken, clearAuth } = useAuthStore();
    const { setSettingsModalOpen } = useWorkspaceStore();
    const toast = useToast();

    const handleLogout = async () => {
        try {
            await authService.logout();
            clearAuth();
            navigate('/');
        } catch (error) {
            console.error("Logout failed", error);
            toast.error(resolveErrorMessage(error, t(I18N_KEYS.COMMON.LOGOUT_FAILED)));
        }
    };

    const userInitial = user?.username?.charAt(0).toUpperCase() || user?.email?.charAt(0).toUpperCase() || "?";

    return (
        <header className="h-12 border-b theme-border bg-[color:var(--bg-toolbar)] px-4 select-none shrink-0">
            <div className="relative h-full flex items-center justify-between">
                <div className="flex-1 min-w-0" />

                <button
                    onClick={() => navigate("/")}
                    className="absolute left-1/2 -translate-x-1/2 px-3 py-1 rounded-md text-sm font-medium theme-text-primary hover:bg-white/5 transition-colors"
                    title={t(I18N_KEYS.AI.BOT_NAME)}
                    aria-label={t(I18N_KEYS.AI.BOT_NAME)}
                    type="button"
                >
                    {t(I18N_KEYS.AI.BOT_NAME)}
                </button>

                <div className="flex flex-1 justify-end items-center gap-3 theme-text-secondary min-w-0">
                    {accessToken ? (
                        <div className="flex items-center gap-3 min-w-0">
                            <button
                                onClick={() => navigate("/profile")}
                                className="flex items-center justify-center rounded-full hover:theme-text-primary transition-colors"
                                title={t(I18N_KEYS.COMMON.PROFILE)}
                                type="button"
                            >
                                {user?.avatarUrl ? (
                                    <img src={user.avatarUrl} alt={user.username} className="h-5 w-5 rounded-full object-cover" />
                                ) : (
                                    <div className="h-6 w-6 rounded-full border theme-border bg-[color:var(--bg-popup)] flex items-center justify-center text-[10px] font-bold theme-text-primary">
                                        {userInitial}
                                    </div>
                                )}
                            </button>

                            <button
                                onClick={handleLogout}
                                className="flex items-center justify-center hover:theme-text-primary transition-colors"
                                title={t(I18N_KEYS.COMMON.LOGOUT)}
                                type="button"
                            >
                                <LogOut className="h-3.5 w-3.5" />
                            </button>
                        </div>
                    ) : (
                        <Button variant="ghost" size="sm" onClick={onLoginClick} className="h-7 text-xs px-2.5">
                            {t(I18N_KEYS.COMMON.LOGIN)}
                        </Button>
                    )}

                    <button
                        onClick={onToggleAI}
                        className="flex items-center justify-center hover:theme-text-primary transition-colors"
                        title={`${t(I18N_KEYS.COMMON.AI_ASSISTANT)} (Cmd+B)`}
                        type="button"
                    >
                        <Wand2 className="h-4 w-4 text-[var(--accent-blue)]" />
                    </button>

                    <button
                        onClick={() => setSettingsModalOpen(true)}
                        className="flex items-center justify-center hover:theme-text-primary transition-colors"
                        title={`${t(I18N_KEYS.COMMON.SETTINGS)} (Cmd+Shift+,)`}
                        type="button"
                    >
                        <Settings className="h-4 w-4" />
                    </button>
                </div>
            </div>
        </header>
    );
}
