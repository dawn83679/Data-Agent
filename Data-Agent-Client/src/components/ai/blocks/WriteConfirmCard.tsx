import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle } from 'lucide-react';
import { WriteConfirmPayload } from './writeConfirmTypes';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { SqlCodeBlock } from '../../common/SqlCodeBlock';
import { confirmWriteOperation, cancelWriteOperation } from '../../../services/writeConfirmationApi';
import { useAIAssistantContext } from '../AIAssistantContext';
import { connectionService } from '../../../services/connection.service';

const AUTO_APPROVE_PREFIX = 'autoApprove_';

// Helper functions for auto-approve localStorage management
const isAutoApproved = (connectionId: number): boolean => {
    return localStorage.getItem(`${AUTO_APPROVE_PREFIX}${connectionId}`) === 'true';
};

const setAutoApprove = (connectionId: number, approve: boolean): void => {
    const key = `${AUTO_APPROVE_PREFIX}${connectionId}`;
    if (approve) {
        localStorage.setItem(key, 'true');
    } else {
        localStorage.removeItem(key);
    }
};

export interface WriteConfirmCardProps {
    payload: WriteConfirmPayload;
    submittedAnswer?: string; // If 'User confirmed' or 'User cancelled' exists in the message flow
}

export function WriteConfirmCard({ payload, submittedAnswer }: WriteConfirmCardProps) {
    const { t } = useTranslation();
    const { submitMessage, isLoading } = useAIAssistantContext();
    const [isProcessing, setIsProcessing] = useState(false);
    const [isSubmitted, setIsSubmitted] = useState(false);
    const [supplementaryInput, setSupplementaryInput] = useState('');
    const [rememberChoice, setRememberChoice] = useState(false);
    const [showAutoApproveNotice, setShowAutoApproveNotice] = useState(true);

    // Check if this connection is already auto-approved
    const isAlreadyAutoApproved = isAutoApproved(payload.connectionId);

    // Fetch connection info to get the connection name
    const { data: connection } = useQuery({
        queryKey: ['connection', payload.connectionId],
        queryFn: () => connectionService.getConnectionById(payload.connectionId),
        enabled: !!payload.connectionId,
        staleTime: 5 * 60 * 1000, // Cache for 5 minutes
    });

    // Auto-confirm if this connection is already approved
    useEffect(() => {
        if (isAlreadyAutoApproved && !isSubmitted && !isProcessing && !submittedAnswer) {
            handleConfirm();
        }
    }, [isAlreadyAutoApproved]); // Only run once when component mounts

    const handleConfirm = async () => {
        if (isProcessing || isLoading) return;
        if (supplementaryInput.trim()) {
            return handleCancel();
        }
        setIsProcessing(true);
        try {
            // Save auto-approve preference if user checked the box
            if (rememberChoice) {
                setAutoApprove(payload.connectionId, true);
            }

            await confirmWriteOperation(payload.confirmationToken, supplementaryInput);
            const msg = supplementaryInput.trim()
                ? t(I18N_KEYS.AI.WRITE_CONFIRM.CONFIRM_WITH_INPUT_MESSAGE, { info: supplementaryInput })
                : t(I18N_KEYS.AI.WRITE_CONFIRM.CONFIRM_MESSAGE);

            submitMessage(msg);
            setIsSubmitted(true);
        } catch {
            submitMessage(t(I18N_KEYS.AI.WRITE_CONFIRM.CONFIRM_FAILED));
        } finally {
            setIsProcessing(false);
        }
    };

    const handleCancel = async () => {
        if (isProcessing || isLoading) return;
        setIsProcessing(true);
        try {
            await cancelWriteOperation(payload.confirmationToken, supplementaryInput);
        } catch {
            // Ignore token expiry / cancellation errors
        } finally {
            const msg = supplementaryInput.trim()
                ? t(I18N_KEYS.AI.WRITE_CONFIRM.CANCEL_WITH_INPUT_MESSAGE, { info: supplementaryInput })
                : t(I18N_KEYS.AI.WRITE_CONFIRM.CANCEL_MESSAGE);

            submitMessage(msg);
            setIsSubmitted(true);
            setIsProcessing(false);
        }
    };

    if (submittedAnswer || isSubmitted) {
        return null;
    }

    // If auto-approved and auto-confirming, show a simple notice
    if (isAlreadyAutoApproved && isProcessing) {
        return (
            <div className="mb-2 p-3 rounded-lg border border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-green-600 dark:text-green-400 animate-pulse" />
                <span className="text-[12px] text-green-700 dark:text-green-300">
                    自动执行中...（此数据源已设置为默认允许）
                </span>
            </div>
        );
    }

    const hasDatabase = !!payload.databaseName;
    const hasSchema = !!payload.schemaName;
    
    // Build path: connectionName->databaseName->schemaName
    const connectionName = connection?.name || `#${payload.connectionId}`;
    const pathParts = [connectionName];
    if (payload.databaseName) pathParts.push(payload.databaseName);
    if (payload.schemaName) pathParts.push(payload.schemaName);
    const connectionPath = pathParts.join('->');
    
    const target = hasDatabase || hasSchema
        ? [payload.databaseName, payload.schemaName].filter(Boolean).join('.')
        : undefined;

    return (
        <div className="mb-2 p-4 rounded-lg border theme-border theme-bg-main shadow-sm flex flex-col gap-3">
            {isAlreadyAutoApproved && showAutoApproveNotice && (
                <div className="flex items-center justify-between px-3 py-2 rounded-md bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800">
                    <div className="flex items-center gap-2">
                        <AlertCircle className="w-3.5 h-3.5 text-green-600 dark:text-green-400" />
                        <span className="text-[11px] text-green-700 dark:text-green-300">
                            此数据源已设置为默认允许操作
                        </span>
                    </div>
                    <button
                        type="button"
                        onClick={() => {
                            setAutoApprove(payload.connectionId, false);
                            setShowAutoApproveNotice(false);
                        }}
                        className="text-[10px] text-green-600 dark:text-green-400 hover:text-green-800 dark:hover:text-green-200 underline"
                    >
                        取消设置
                    </button>
                </div>
            )}

            <div className="flex items-center gap-2 text-amber-600 dark:text-amber-500 font-medium">
                <AlertCircle className="w-4 h-4" />
                <span className="text-[11px] uppercase tracking-wide">{t(I18N_KEYS.AI.WRITE_CONFIRM.LABEL_ACTION)}</span>
            </div>

            {payload.explanation && (
                <p className="theme-text-primary text-[13px] whitespace-pre-wrap">
                    {payload.explanation}
                </p>
            )}

            {payload.sqlPreview && (
                <div className="rounded border theme-border theme-bg-main overflow-hidden">
                    <div className="theme-bg-panel px-2 py-1 text-xs font-medium border-b theme-border flex justify-between">
                        <span>{t(I18N_KEYS.AI.WRITE_CONFIRM.SQL_PREVIEW_LABEL)}</span>
                        <span className="opacity-60 font-mono">{connectionPath}</span>
                    </div>
                    <SqlCodeBlock variant="compact" sql={payload.sqlPreview} wrapLongLines={true} />
                </div>
            )}

            {target && (
                <p className="text-[12px] theme-text-secondary mb-2">
                    {t(I18N_KEYS.AI.WRITE_CONFIRM.TARGET_LABEL)}:{' '}
                    <code className="font-mono theme-text-primary">{target}</code>
                </p>
            )}

            <div className="flex flex-col gap-2 mt-2">
                <div className="flex items-center gap-2 px-2 py-1.5">
                    <input
                        type="checkbox"
                        id="rememberChoice"
                        checked={rememberChoice}
                        onChange={(e) => setRememberChoice(e.target.checked)}
                        disabled={isProcessing || isLoading}
                        className="w-3.5 h-3.5 rounded border-gray-300 text-green-600 focus:ring-green-500 focus:ring-offset-0 focus:ring-1 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                    />
                    <label
                        htmlFor="rememberChoice"
                        className="text-[11px] theme-text-secondary cursor-pointer select-none"
                    >
                        默认允许对此数据源的操作（不再询问）
                    </label>
                </div>
                <button
                    type="button"
                    onClick={handleConfirm}
                    disabled={isProcessing || isLoading || payload.error}
                    className="w-full px-3 py-1.5 rounded-md text-[12px] font-medium bg-green-600 hover:bg-green-700 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {t(I18N_KEYS.AI.WRITE_CONFIRM.YES_BTN)}
                </button>
                <button
                    type="button"
                    onClick={handleCancel}
                    disabled={isProcessing || isLoading}
                    className="w-full px-3 py-1.5 rounded-md text-[12px] font-medium bg-red-600 hover:bg-red-700 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {t(I18N_KEYS.AI.WRITE_CONFIRM.NO_BTN)}
                </button>
                <input
                    type="text"
                    value={supplementaryInput}
                    onChange={(e) => setSupplementaryInput(e.target.value)}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' && !isProcessing && !isLoading && !payload.error) {
                            supplementaryInput.trim() ? handleCancel() : handleConfirm();
                        }
                    }}
                    placeholder={t(I18N_KEYS.AI.WRITE_CONFIRM.INPUT_PLACEHOLDER)}
                    disabled={isProcessing || isLoading}
                    className="w-full mt-1 px-2.5 py-1.5 rounded-md border theme-border theme-bg-panel theme-text-primary text-[12px] placeholder:theme-text-secondary focus:outline-none focus:ring-1 focus:ring-[var(--accent-blue)] focus:border-[var(--accent-blue)] disabled:opacity-60 disabled:cursor-not-allowed"
                />
            </div>
        </div>
    );
}
