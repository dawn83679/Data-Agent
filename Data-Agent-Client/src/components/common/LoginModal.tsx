import { useState } from "react";
import { Button } from "../ui/Button";
import { Input } from "../ui/Input";
import { DialogContent, DialogDescription, DialogHeader, DialogTitle } from "../ui/Dialog";
import { Github, Eye, EyeOff } from "lucide-react";
import { authService } from "../../services/auth.service";
import { Alert } from "../ui/Alert";
import { useAuthStore } from "../../store/authStore";
import { resolveErrorMessage } from "../../lib/errorMessage";

interface LoginModalProps {
    onSwitchToRegister: () => void;
    onClose: () => void;
}

export function LoginModal({ onSwitchToRegister, onClose }: LoginModalProps) {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [rememberMe, setRememberMe] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const { setAuth } = useAuthStore();

    const handleLogin = async (e?: React.FormEvent) => {
        e?.preventDefault();
        try {
            setError(null);
            setLoading(true);
            const response = await authService.login({ email, password, rememberMe });
            setAuth(null, response.accessToken, response.refreshToken, rememberMe);
            onClose(); // Close modal after successful login
        } catch (error) {
            console.error("Login failed", error);
            setError(resolveErrorMessage(error, "Login failed. Please check your credentials."));
        } finally {
            setLoading(false);
        }
    };

    return (
        <DialogContent className="sm:max-w-[425px]">
            <DialogHeader>
                <DialogTitle className="text-2xl text-center">Login</DialogTitle>
                <DialogDescription className="text-center">
                    Enter your email and password to access your account
                </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleLogin} className="grid gap-4 py-4">
                {error && (
                    <Alert variant="destructive">
                        {error}
                    </Alert>
                )}
                <div className="grid gap-2">
                    <label htmlFor="email" className="text-sm font-medium text-foreground">Email</label>
                    <Input
                        id="email"
                        type="email"
                        placeholder="m@example.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        autoComplete="email"
                    />
                </div>
                <div className="grid gap-2">
                    <label htmlFor="password" className="text-sm font-medium text-foreground">Password</label>
                    <div className="relative">
                        <Input
                            id="password"
                            type={showPassword ? "text" : "password"}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="current-password"
                            className="pr-10"
                        />
                        <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                            aria-label={showPassword ? "Hide password" : "Show password"}
                        >
                            {showPassword ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </button>
                    </div>
                </div>
                <div className="flex items-center space-x-2">
                    <input
                        type="checkbox"
                        id="remember"
                        className="h-4 w-4 rounded border border-input bg-background text-primary accent-primary focus:ring-ring"
                        checked={rememberMe}
                        onChange={(e) => setRememberMe(e.target.checked)}
                    />
                    <label
                        htmlFor="remember"
                        className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 text-foreground"
                    >
                        Remember me
                    </label>
                </div>
                <Button type="submit" className="w-full" disabled={loading}>
                    {loading ? "Signing In..." : "Sign In"}
                </Button>
                <div className="relative">
                    <div className="absolute inset-0 flex items-center">
                        <span className="w-full border-t border-border" />
                    </div>
                    <div className="relative flex justify-center text-xs uppercase">
                        <span className="bg-background px-2 text-muted-foreground">
                            Or continue with
                        </span>
                    </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                    <Button variant="outline" asChild>
                        <a href={authService.getOAuthUrl('github', window.location.href)}>
                            <Github className="mr-2 h-4 w-4" />
                            Github
                        </a>
                    </Button>
                    <Button
                        variant="outline"
                        onClick={() => {
                            window.location.href = `/api/oauth/google?fromUrl=${encodeURIComponent(window.location.href)}`;
                        }}
                    >
                        <svg className="mr-2 h-4 w-4" viewBox="0 0 24 24">
                            <path
                                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                                fill="#4285F4"
                            />
                            <path
                                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                                fill="#34A853"
                            />
                            <path
                                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"
                                fill="#FBBC05"
                            />
                            <path
                                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                                fill="#EA4335"
                            />
                        </svg>
                        Google
                    </Button>
                </div>
            </form>
            <div className="text-sm text-center text-muted-foreground">
                Don't have an account?{" "}
                <button className="text-primary hover:underline" onClick={onSwitchToRegister}>
                    Sign up
                </button>
            </div>
        </DialogContent>
    );
}
