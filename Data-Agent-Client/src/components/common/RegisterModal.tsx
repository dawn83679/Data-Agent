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
import { I18N_KEYS } from "../../constants/i18nKeys";

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
            setError(t(I18N_KEYS.AUTH.FILL_ALL));
            return;
        }

        if (password !== confirmPassword) {
            setError(t(I18N_KEYS.AUTH.PASSWORDS_NO_MATCH));
            return;
        }

        if (password.length < 6) {
            setError(t(I18N_KEYS.AUTH.PASSWORD_MIN_LENGTH));
            return;
        }

        try {
            setLoading(true);
            await authService.register({ username, email, password });
            toast.success(t(I18N_KEYS.AUTH.REGISTER_SUCCESS));
            onSwitchToLogin();
        } catch (error) {
            console.error("Registration failed", error);
            setError(resolveErrorMessage(error, t(I18N_KEYS.AUTH.REGISTER_FAILED)));
        } finally {
            setLoading(false);
        }
    };

    return (
        <DialogContent className="sm:max-w-[425px]">
            <DialogHeader>
                <DialogTitle className="text-2xl text-center">{t(I18N_KEYS.AUTH.SIGN_UP_TITLE)}</DialogTitle>
                <DialogDescription className="text-center">
                    {t(I18N_KEYS.AUTH.SIGN_UP_DESC)}
                </DialogDescription>
            </DialogHeader>
            <div className="grid gap-4 py-4">
                {error && (
                    <Alert variant="destructive">
                        {error}
                    </Alert>
                )}
                <div className="grid gap-2">
                    <label htmlFor="username" className="text-sm font-medium text-foreground">{t(I18N_KEYS.AUTH.USERNAME)}</label>
                    <Input
                        id="username"
                        type="text"
                        placeholder={t(I18N_KEYS.AUTH.USERNAME_PLACEHOLDER)}
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                    />
                </div>
                <div className="grid gap-2">
                    <label htmlFor="email" className="text-sm font-medium text-foreground">{t(I18N_KEYS.AUTH.EMAIL)}</label>
                    <Input
                        id="email"
                        type="email"
                        placeholder={t(I18N_KEYS.AUTH.EMAIL_PLACEHOLDER)}
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                    />
                </div>
                <div className="grid gap-2">
                    <label htmlFor="password" className="text-sm font-medium text-foreground">{t(I18N_KEYS.AUTH.PASSWORD)}</label>
                    <div className="relative">
                        <Input
                            id="password"
                            type={showPassword ? "text" : "password"}
                            placeholder={t(I18N_KEYS.AUTH.PASSWORD_PLACEHOLDER)}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="pr-10"
                        />
                        <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                            aria-label={showPassword ? t(I18N_KEYS.AUTH.HIDE_PASSWORD) : t(I18N_KEYS.AUTH.SHOW_PASSWORD)}
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
                    <label htmlFor="confirm-password" className="text-sm font-medium text-foreground">{t(I18N_KEYS.AUTH.CONFIRM_PASSWORD)}</label>
                    <div className="relative">
                        <Input
                            id="confirm-password"
                            type={showConfirmPassword ? "text" : "password"}
                            placeholder={t(I18N_KEYS.AUTH.CONFIRM_PASSWORD_PLACEHOLDER)}
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            className="pr-10"
                        />
                        <button
                            type="button"
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                            aria-label={showConfirmPassword ? t(I18N_KEYS.AUTH.HIDE_PASSWORD) : t(I18N_KEYS.AUTH.SHOW_PASSWORD)}
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
                    {loading ? t(I18N_KEYS.AUTH.CREATING_ACCOUNT) : t(I18N_KEYS.AUTH.CREATE_ACCOUNT)}
                </Button>
            </div>
            <div className="text-sm text-center text-muted-foreground">
                {t(I18N_KEYS.AUTH.HAS_ACCOUNT)}{" "}
                <button className="text-primary hover:underline" onClick={onSwitchToLogin}>
                    {t(I18N_KEYS.AUTH.SIGN_IN_LINK)}
                </button>
            </div>
        </DialogContent>
    );
}
