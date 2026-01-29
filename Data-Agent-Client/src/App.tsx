import { useState, useEffect } from "react";
import { BrowserRouter as Router, useRoutes } from "react-router-dom";
import { Dialog } from "./components/ui/Dialog";
import { LoginModal } from "./components/common/LoginModal";
import { RegisterModal } from "./components/common/RegisterModal";
import { ThemeProvider } from "./hooks/useTheme";
import { Header } from "./components/layouts/Header";
import { ToastContainer } from "./components/ui/Toast";
import { setLoginModalCallback } from "./store/authStore";
import { routerConfig } from "./router.tsx";
import { useOAuthCallbackFromUrl } from "./hooks/useOAuthCallbackFromUrl";

function AppRoutes() {
    const element = useRoutes(routerConfig);
    return element;
}

function App() {
    const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
    const [modalType, setModalType] = useState<"login" | "register">("login");

    // 处理 OAuth 登录回调：从 URL 读取 token 同步到 authStore
    useOAuthCallbackFromUrl();

    useEffect(() => {
        setLoginModalCallback(() => {
            setModalType("login");
            setIsAuthModalOpen(true);
        });
    }, []);

    const handleSwitchToRegister = () => {
        setModalType("register");
    };

    const handleSwitchToLogin = () => {
        setModalType("login");
    };

    return (
        <ThemeProvider>
            <Router>
                <div className="min-h-screen bg-background text-foreground transition-colors duration-300">
                    <Header onLoginClick={() => {
                        setModalType("login");
                        setIsAuthModalOpen(true);
                    }} />
                    <main className="container mx-auto px-4 py-8">
                        <AppRoutes />
                        <Dialog open={isAuthModalOpen} onOpenChange={setIsAuthModalOpen}>
                            {modalType === "login" ? (
                                <LoginModal
                                    onSwitchToRegister={handleSwitchToRegister}
                                    onClose={() => setIsAuthModalOpen(false)}
                                />
                            ) : (
                                <RegisterModal onSwitchToLogin={handleSwitchToLogin} />
                            )}
                        </Dialog>
                    </main>
                    <ToastContainer />
                </div>
            </Router>
        </ThemeProvider>
    );
}

export default App;
