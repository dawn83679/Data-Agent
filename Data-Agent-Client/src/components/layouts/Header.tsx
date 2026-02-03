import { useNavigate } from "react-router-dom";
import { Button } from "../ui/Button";
import { useAuthStore } from "../../store/authStore";
import { useWorkspaceStore } from "../../store/workspaceStore";
import { authService } from "../../services/auth.service";
import { LogOut, Settings, Wand2 } from "lucide-react";
import { resolveErrorMessage } from "../../lib/errorMessage";
import { useToast } from "../../hooks/useToast";
import { useTranslation } from "react-i18next";

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
            toast.error(resolveErrorMessage(error, t('common.logout_failed')));
        }
    };

    const userInitial = user?.username?.charAt(0).toUpperCase() || user?.email?.charAt(0).toUpperCase() || "?";

    return (
        <header className="h-10 theme-bg-panel flex items-center px-4 border-b theme-border justify-between select-none shrink-0">
            <div className="flex items-center gap-2">
                <span className="text-sm font-bold bg-gradient-to-r from-blue-600 to-blue-400 bg-clip-text text-transparent cursor-pointer" onClick={() => navigate("/")}>
                    {t('ai.bot_name')}
                </span>
            </div>

            <div className="flex items-center gap-2">
                {accessToken ? (
                    <div className="flex items-center gap-2">
                        <button
                            onClick={() => navigate("/profile")}
                            className="flex items-center gap-2 px-2 py-1 rounded-md theme-bg-hover transition-colors group"
                            title={t('common.profile')}
                        >
                            {user?.avatarUrl ? (
                                <img src={user.avatarUrl} alt={user.username} className="h-5 w-5 rounded-full object-cover" />
                            ) : (
                                <div className="h-5 w-5 rounded-full bg-primary/10 flex items-center justify-center text-[10px] font-bold text-primary">
                                    {userInitial}
                                </div>
                            )}
                            <span className="text-xs font-medium hidden sm:inline-block max-w-[100px] truncate theme-text-secondary group-hover:theme-text-primary">
                                {user?.username || user?.email}
                            </span>
                        </button>

                        <button 
                            onClick={handleLogout}
                            className="p-1.5 rounded theme-bg-hover theme-text-secondary hover:text-red-400 transition-colors"
                            title={t('common.logout')}
                        >
                            <LogOut className="h-3.5 w-3.5" />
                        </button>
                    </div>
                ) : (
                    <Button variant="ghost" size="sm" onClick={onLoginClick} className="h-7 text-xs">
                        {t('common.login')}
                    </Button>
                )}
                
                <div className="w-px h-4 bg-border mx-1" />
                
                <button 
                    onClick={onToggleAI}
                    className="w-7 h-7 flex items-center justify-center rounded theme-bg-hover text-purple-400 transition-colors"
                    title={`${t('common.ai_assistant')} (Cmd+B)`}
                >
                    <Wand2 className="h-4 w-4" />
                </button>
                
                <button 
                    onClick={() => setSettingsModalOpen(true)}
                    className="w-7 h-7 flex items-center justify-center rounded theme-bg-hover theme-text-secondary hover:theme-text-primary transition-colors"
                    title={`${t('common.settings')} (Cmd+Shift+,)`}
                >
                    <Settings className="h-4 w-4" />
                </button>
            </div>
        </header>
    );
}
