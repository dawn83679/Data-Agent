import { useState } from "react";
import { Button } from "../ui/Button";
import { Input } from "../ui/Input";
import { DialogContent, DialogDescription, DialogHeader, DialogTitle } from "../ui/Dialog";
import { Eye, EyeOff } from "lucide-react";
import { authService } from "../../services/auth.service";
import { Alert } from "../ui/Alert";
import { useToast } from "../../hooks/useToast";
import { resolveErrorMessage } from "../../lib/errorMessage";
import { useTranslation } from "react-i18next";

interface RegisterModalProps {
    onSwitchToLogin: () => void;
}

export function RegisterModal({ onSwitchToLogin }: RegisterModalProps) {
    const { t } = useTranslation();
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const toast = useToast();

    const handleRegister = async () => {
        setError(null);
        // Validation
        if (!username || !email || !password) {
            setError(t('auth.fill_all'));
            return;
        }

        if (password !== confirmPassword) {
            setError(t('auth.passwords_no_match'));
            return;
        }

        if (password.length < 6) {
            setError(t('auth.password_min_length'));
            return;
        }

        try {
            setLoading(true);
            await authService.register({ username, email, password });
            toast.success(t('auth.register_success'));
            onSwitchToLogin();
        } catch (error) {
            console.error("Registration failed", error);
            setError(resolveErrorMessage(error, t('auth.register_failed')));
        } finally {
            setLoading(false);
        }
    };

    return (
        <DialogContent className="sm:max-w-[425px]">
            <DialogHeader>
                <DialogTitle className="text-2xl text-center">{t('auth.sign_up_title')}</DialogTitle>
                <DialogDescription className="text-center">
                    {t('auth.sign_up_desc')}
                </DialogDescription>
            </DialogHeader>
            <div className="grid gap-4 py-4">
                {error && (
                    <Alert variant="destructive">
                        {error}
                    </Alert>
                )}
                <div className="grid gap-2">
                    <label htmlFor="username" className="text-sm font-medium text-foreground">{t('auth.username')}</label>
                    <Input
                        id="username"
                        type="text"
                        placeholder={t('auth.username_placeholder')}
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                    />
                </div>
                <div className="grid gap-2">
                    <label htmlFor="email" className="text-sm font-medium text-foreground">{t('auth.email')}</label>
                    <Input
                        id="email"
                        type="email"
                        placeholder={t('auth.email_placeholder')}
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                    />
                </div>
                <div className="grid gap-2">
                    <label htmlFor="password" className="text-sm font-medium text-foreground">{t('auth.password')}</label>
                    <div className="relative">
                        <Input
                            id="password"
                            type={showPassword ? "text" : "password"}
                            placeholder={t('auth.password_placeholder')}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="pr-10"
                        />
                        <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                            aria-label={showPassword ? t('auth.hide_password') : t('auth.show_password')}
                        >
                            {showPassword ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </button>
                    </div>
                </div>
                <div className="grid gap-2">
                    <label htmlFor="confirm-password" className="text-sm font-medium text-foreground">{t('auth.confirm_password')}</label>
                    <div className="relative">
                        <Input
                            id="confirm-password"
                            type={showConfirmPassword ? "text" : "password"}
                            placeholder={t('auth.confirm_password_placeholder')}
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            className="pr-10"
                        />
                        <button
                            type="button"
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                            aria-label={showConfirmPassword ? t('auth.hide_password') : t('auth.show_password')}
                        >
                            {showConfirmPassword ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </button>
                    </div>
                </div>
                <Button className="w-full" onClick={handleRegister} disabled={loading}>
                    {loading ? t('auth.creating_account') : t('auth.create_account')}
                </Button>
            </div>
            <div className="text-sm text-center text-muted-foreground">
                {t('auth.has_account')}{" "}
                <button className="text-primary hover:underline" onClick={onSwitchToLogin}>
                    {t('auth.sign_in_link')}
                </button>
            </div>
        </DialogContent>
    );
}
