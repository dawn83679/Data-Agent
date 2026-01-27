import { useState } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import { Dialog } from "./components/ui/Dialog";
import { LoginModal } from "./components/common/LoginModal";
import { ThemeProvider } from "./hooks/useTheme";
import { Header } from "./components/layouts/Header";

function App() {
    const [isLoginOpen, setIsLoginOpen] = useState(false);

    return (
        <ThemeProvider>
            <Router>
                <div className="min-h-screen bg-background text-foreground transition-colors duration-300">
                    <Header onLoginClick={() => setIsLoginOpen(true)} />
                    <main className="container mx-auto px-4 py-8">
                        <Dialog open={isLoginOpen} onOpenChange={setIsLoginOpen}>
                            <Routes>
                                <Route path="/" element={<Home />} />
                            </Routes>
                            <LoginModal />
                        </Dialog>
                    </main>
                </div>
            </Router>
        </ThemeProvider>
    );
}

export default App;
