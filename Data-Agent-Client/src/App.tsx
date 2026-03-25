import { useState, useEffect, useCallback } from "react";
import { BrowserRouter as Router, useRoutes, useLocation } from "react-router-dom";
import { Dialog } from "./components/ui/Dialog";
import { LoginModal } from "./components/common/LoginModal";
import { RegisterModal } from "./components/common/RegisterModal";
import { SettingsModal } from "./components/common/SettingsModal";
import { ThemeProvider } from "./hooks/useTheme";
import { Header } from "./components/layouts/Header";
import { ToastContainer } from "./components/ui/Toast";
import { useAuthStore } from "./store/authStore";
import { useWorkspaceStore } from "./store/workspaceStore";
import { routerConfig } from "./router.tsx";
import { ROUTES } from "./constants/routes";
import { AuthModalType } from "./constants/modal";
import { DataAttributes } from "./constants/dataAttributes";
import { useOAuthCallbackFromUrl } from "./hooks/useOAuthCallbackFromUrl";
import { WorkspaceLayout } from "./components/layouts/WorkspaceLayout";

function AppRoutes({
    showAI,
    toggleAISidebar,
    showExplorer,
    toggleExplorer
}: {
    showAI: boolean;
    toggleAISidebar: () => void;
    showExplorer: boolean;
    toggleExplorer: () => void;
}) {
    const element = useRoutes(routerConfig);
    const location = useLocation();
    
    // Show IDE layout (sidebar + AI assistant) only at root path "/"
    const isWorkspace = location.pathname === ROUTES.HOME;

    if (!isWorkspace) {
        return (
            <div className="flex-1 flex flex-col overflow-hidden bg-background">
                <main className="flex-1 overflow-y-auto p-8 animate-fade-in">
                    {element}
                </main>
            </div>
        );
    }

    return (
        <WorkspaceLayout 
            showAI={showAI} 
            onToggleAI={toggleAISidebar}
            showExplorer={showExplorer}
            onToggleExplorer={toggleExplorer}
        >
            {element}
        </WorkspaceLayout>
    );
}

function App() {
    const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
    const [modalType, setModalType] = useState<AuthModalType>(AuthModalType.LOGIN);
    const { isLoginModalOpen, closeLoginModal } = useAuthStore();
    const { setSettingsModalOpen } = useWorkspaceStore();

    // Layout state
    const [showAI, setShowAI] = useState(false);
    const [showExplorer, setShowExplorer] = useState(true);

    // OAuth callback: read token from URL and sync to authStore
    useOAuthCallbackFromUrl();

    // Fetch supported DB types on mount for connection creation etc.
    useEffect(() => {
        useWorkspaceStore.getState().fetchSupportedDbTypes();
    }, []);

    // Disable browser context menu except where custom menus exist (e.g. Explorer ContextMenu)
    useEffect(() => {
        const handleContextMenu = (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            // Allow Radix context menus: trigger is inside [data-radix-context-menu-trigger] or similar
            if (target.closest('[data-radix-popper-content-wrapper]') ||
                target.closest('[role="menu"]') ||
                target.closest('[data-explorer-context-menu]')) {
                return;
            }
            // Allow Explorer tree nodes - they use ContextMenu for "View DDL"
            if (target.closest(`[${DataAttributes.EXPLORER_TREE}]`)) {
                return;
            }
            e.preventDefault();
        };
        document.addEventListener('contextmenu', handleContextMenu);
        return () => document.removeEventListener('contextmenu', handleContextMenu);
    }, []);

    const handleSwitchToRegister = () => {
        setModalType(AuthModalType.REGISTER);
    };

    const handleSwitchToLogin = () => {
        setModalType(AuthModalType.LOGIN);
    };

    const toggleAISidebar = useCallback(() => {
        setShowAI(prev => {
            const newState = !prev;
            if (newState) {
                setTimeout(() => {
                    const aiInput = document.querySelector(`textarea[${DataAttributes.AI_INPUT}]`) as HTMLTextAreaElement;
                    aiInput?.focus();
                }, 100);
            }
            return newState;
        });
    }, []);

    const toggleExplorer = useCallback(() => {
        setShowExplorer(prev => !prev);
    }, []);

    // Global keyboard shortcuts
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            // Cmd+B or Ctrl+B: Toggle AI Sidebar
            if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'b') {
                e.preventDefault();
                toggleAISidebar();
            }

            // ESC: Toggle Database Explorer (if not in a text input)
            if (e.key === 'Escape') {
                const activeEl = document.activeElement;
                const isInput = activeEl?.tagName === 'INPUT' || 
                               activeEl?.tagName === 'TEXTAREA' || 
                               (activeEl as HTMLElement)?.isContentEditable;
                
                if (!isInput) {
                    e.preventDefault();
                    toggleExplorer();
                }
            }

            // Cmd+Shift+, or Ctrl+Shift+,: Open Settings
            if ((e.metaKey || e.ctrlKey) && e.shiftKey && (e.key === ',' || e.code === 'Comma')) {
                e.preventDefault();
                setSettingsModalOpen(true);
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [toggleAISidebar, toggleExplorer, setSettingsModalOpen]);

    return (
        <ThemeProvider>
            <Router>
                <div className="h-screen flex flex-col theme-bg-main theme-text-primary overflow-hidden">
                    <Header 
                        onLoginClick={() => {
                            setModalType(AuthModalType.LOGIN);
                            setIsAuthModalOpen(true);
                        }}
                    />
                    
                    <AppRoutes 
                        showAI={showAI}
                        toggleAISidebar={toggleAISidebar}
                        showExplorer={showExplorer}
                        toggleExplorer={toggleExplorer}
                    />

                    <Dialog
                        open={isAuthModalOpen || isLoginModalOpen}
                        onOpenChange={(open) => {
                            setIsAuthModalOpen(open);
                            if (!open) {
                                closeLoginModal();
                            } else if (open && isLoginModalOpen) {
                                setModalType(AuthModalType.LOGIN);
                            }
                        }}
                    >
                        {modalType === AuthModalType.LOGIN ? (
                            <LoginModal
                                onSwitchToRegister={handleSwitchToRegister}
                                onClose={() => {
                                    setIsAuthModalOpen(false);
                                    closeLoginModal();
                                }}
                            />
                        ) : (
                            <RegisterModal onSwitchToLogin={handleSwitchToLogin} />
                        )}
                    </Dialog>
                    <ToastContainer />
                    <SettingsModal />
                </div>
            </Router>
        </ThemeProvider>
    );
}

export default App;
