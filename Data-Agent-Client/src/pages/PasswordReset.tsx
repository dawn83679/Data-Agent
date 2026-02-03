import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore';
import { authService } from '../services/auth.service';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Lock, Save, Eye, EyeOff } from 'lucide-react';
import { resolveErrorMessage } from '../lib/errorMessage';
import { useToast } from '../hooks/useToast';
import { useNavigate } from 'react-router-dom';

export default function PasswordReset() {
    const { t } = useTranslation();
    const { user, clearAuth } = useAuthStore();
    const navigate = useNavigate();
    const toast = useToast();
    
    const [email, setEmail] = useState(user?.email || '');
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showOldPassword, setShowOldPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [errors, setErrors] = useState<Record<string, string>>({});

    // 验证密码格式：至少8位，包含字母和数字
    const validatePasswordFormat = (password: string): boolean => {
        return password.length >= 8 && /[A-Za-z]/.test(password) && /\d/.test(password);
    };

    // 验证邮箱格式
    const validateEmail = (email: string): boolean => {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    };

    const validateForm = (): boolean => {
        const newErrors: Record<string, string> = {};

        if (!email) {
            newErrors.email = t('password.email_required');
        } else if (!validateEmail(email)) {
            newErrors.email = t('password.email_invalid');
        }

        if (!oldPassword) {
            newErrors.oldPassword = t('password.old_required');
        } else if (!validatePasswordFormat(oldPassword)) {
            newErrors.oldPassword = t('password.old_format');
        }

        if (!newPassword) {
            newErrors.newPassword = t('password.new_required');
        } else if (!validatePasswordFormat(newPassword)) {
            newErrors.newPassword = t('password.new_format');
        }

        if (!confirmPassword) {
            newErrors.confirmPassword = t('password.confirm_required');
        } else if (newPassword !== confirmPassword) {
            newErrors.confirmPassword = t('password.confirm_match');
        }

        if (oldPassword && newPassword && oldPassword === newPassword) {
            newErrors.newPassword = t('password.new_different');
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!validateForm()) {
            return;
        }

        setIsLoading(true);
        setErrors({});

        try {
            await authService.resetPassword({
                email,
                oldPassword,
                newPassword,
            });

            toast.success(t('password.success_toast'));

            setTimeout(() => {
                clearAuth();
                navigate('/');
                toast.info(t('password.login_again_toast'));
            }, 2000);
        } catch (error: any) {
            const errorMessage = resolveErrorMessage(error, t('password.reset_failed'));
            toast.error(errorMessage);

            if (error?.response?.status === 401) {
                setErrors({ oldPassword: t('password.old_incorrect') });
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div>
            <div className="mb-6">
                <h2 className="text-xl font-semibold">{t('password.page_title')}</h2>
                <p className="text-sm text-muted-foreground mt-1">
                    {t('password.page_desc')}
                </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
                <div className="space-y-2">
                    <label htmlFor="email" className="text-sm font-medium">
                        {t('password.email_label')}
                    </label>
                    <Input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(e) => {
                            setEmail(e.target.value);
                            if (errors.email) {
                                setErrors({ ...errors, email: '' });
                            }
                        }}
                        placeholder={t('password.email_placeholder')}
                        disabled={!!user?.email}
                        className={errors.email ? 'border-destructive' : ''}
                    />
                    {errors.email && (
                        <p className="text-sm text-destructive">{errors.email}</p>
                    )}
                </div>

                <div className="space-y-2">
                    <label htmlFor="oldPassword" className="text-sm font-medium">
                        {t('password.old_label')}
                    </label>
                    <div className="relative">
                        <Input
                            id="oldPassword"
                            type={showOldPassword ? 'text' : 'password'}
                            value={oldPassword}
                            onChange={(e) => {
                                setOldPassword(e.target.value);
                                if (errors.oldPassword) {
                                    setErrors({ ...errors, oldPassword: '' });
                                }
                            }}
                            placeholder={t('password.old_placeholder')}
                            className={errors.oldPassword ? 'border-destructive pr-10' : 'pr-10'}
                        />
                        <button
                            type="button"
                            onClick={() => setShowOldPassword(!showOldPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        >
                            {showOldPassword ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </button>
                    </div>
                    {errors.oldPassword && (
                        <p className="text-sm text-destructive">{errors.oldPassword}</p>
                    )}
                </div>

                <div className="space-y-2">
                    <label htmlFor="newPassword" className="text-sm font-medium">
                        {t('password.new_label')}
                    </label>
                    <div className="relative">
                        <Input
                            id="newPassword"
                            type={showNewPassword ? 'text' : 'password'}
                            value={newPassword}
                            onChange={(e) => {
                                setNewPassword(e.target.value);
                                if (errors.newPassword) {
                                    setErrors({ ...errors, newPassword: '' });
                                }
                            }}
                            placeholder={t('password.new_placeholder')}
                            className={errors.newPassword ? 'border-destructive pr-10' : 'pr-10'}
                        />
                        <button
                            type="button"
                            onClick={() => setShowNewPassword(!showNewPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        >
                            {showNewPassword ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </button>
                    </div>
                    {errors.newPassword && (
                        <p className="text-sm text-destructive">{errors.newPassword}</p>
                    )}
                    <p className="text-xs text-muted-foreground">
                        {t('password.hint_length')}
                    </p>
                </div>

                <div className="space-y-2">
                    <label htmlFor="confirmPassword" className="text-sm font-medium">
                        {t('password.confirm_label')}
                    </label>
                    <div className="relative">
                        <Input
                            id="confirmPassword"
                            type={showConfirmPassword ? 'text' : 'password'}
                            value={confirmPassword}
                            onChange={(e) => {
                                setConfirmPassword(e.target.value);
                                if (errors.confirmPassword) {
                                    setErrors({ ...errors, confirmPassword: '' });
                                }
                            }}
                            placeholder={t('password.confirm_placeholder')}
                            className={errors.confirmPassword ? 'border-destructive pr-10' : 'pr-10'}
                        />
                        <button
                            type="button"
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        >
                            {showConfirmPassword ? (
                                <EyeOff className="h-4 w-4" />
                            ) : (
                                <Eye className="h-4 w-4" />
                            )}
                        </button>
                    </div>
                    {errors.confirmPassword && (
                        <p className="text-sm text-destructive">{errors.confirmPassword}</p>
                    )}
                </div>

                <div className="bg-muted/50 border border-border rounded-lg p-4">
                    <div className="flex items-start gap-2">
                        <Lock className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
                        <div className="text-sm text-muted-foreground">
                            <p className="font-medium mb-1">{t('password.security_notice_title')}</p>
                            <p>{t('password.security_notice_desc')}</p>
                        </div>
                    </div>
                </div>

                <Button type="submit" disabled={isLoading} className="w-full sm:w-auto">
                    <Save className="h-4 w-4 mr-2" />
                    {isLoading ? t('password.updating') : t('password.change_btn')}
                </Button>
            </form>
        </div>
    );
}
