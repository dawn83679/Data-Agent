import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AlertCircle } from 'lucide-react';
import { WriteConfirmPayload } from './writeConfirmTypes';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { confirmWriteOperation, cancelWriteOperation } from '../../../services/writeConfirmationApi';
import { useAIAssistantContext } from '../AIAssistantContext';

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

    const handleConfirm = async () => {
        if (isProcessing || isLoading) return;
        if (supplementaryInput.trim()) {
            return handleCancel();
        }
        setIsProcessing(true);
        try {
            await confirmWriteOperation(payload.confirmationToken, supplementaryInput);
            let msg = supplementaryInput.trim()
                ? t(I18N_KEYS.AI.WRITE_CONFIRM.CONFIRM_WITH_INPUT_MESSAGE, { info: supplementaryInput })
                : t(I18N_KEYS.AI.WRITE_CONFIRM.CONFIRM_MESSAGE);

            if (payload.sqlPreview) {
                msg += `\n\n${t(I18N_KEYS.AI.WRITE_CONFIRM.EXECUTE_SQL_PREFIX)}\n\`\`\`sql\n${payload.sqlPreview}\n\`\`\``;
            }

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
            let msg = supplementaryInput.trim()
                ? t(I18N_KEYS.AI.WRITE_CONFIRM.CANCEL_WITH_INPUT_MESSAGE, { info: supplementaryInput })
                : t(I18N_KEYS.AI.WRITE_CONFIRM.CANCEL_MESSAGE);

            if (payload.sqlPreview) {
                msg += `\n\n${t(I18N_KEYS.AI.WRITE_CONFIRM.CANCEL_SQL_PREFIX)}\n\`\`\`sql\n${payload.sqlPreview}\n\`\`\``;
            }

            submitMessage(msg);
            setIsSubmitted(true);
            setIsProcessing(false);
        }
    };

    if (submittedAnswer || isSubmitted) {
        return null;
    }

    const hasDatabase = !!payload.databaseName;
    const hasSchema = !!payload.schemaName;
    const target = hasDatabase || hasSchema
        ? [payload.databaseName, payload.schemaName].filter(Boolean).join('.')
        : undefined;

    return (
        <div className="mb-2 p-4 rounded-lg border theme-border theme-bg-main shadow-sm flex flex-col gap-3">
            <div className="flex items-center gap-2 text-amber-600 dark:text-amber-500 font-medium">
                <AlertCircle className="w-4 h-4" />
                <span className="text-[11px] uppercase tracking-wide">{t(I18N_KEYS.AI.WRITE_CONFIRM.LABEL_ACTION)}</span>
            </div>

            <div className="flex flex-wrap items-center gap-2 text-[11px] theme-text-secondary">
                <span className="px-1.5 py-0.5 rounded-full border theme-border/70 bg-amber-50/60 dark:bg-amber-500/10">
                    Conn <code className="font-mono theme-text-primary ml-0.5">#{payload.connectionId}</code>
                </span>
                {hasDatabase && (
                    <span className="px-1.5 py-0.5 rounded-full border theme-border/60">
                        DB <code className="font-mono theme-text-primary ml-0.5">{payload.databaseName}</code>
                    </span>
                )}
                {hasSchema && (
                    <span className="px-1.5 py-0.5 rounded-full border theme-border/60">
                        Schema <code className="font-mono theme-text-primary ml-0.5">{payload.schemaName}</code>
                    </span>
                )}
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
                        {target && <span className="opacity-60">{target}</span>}
                    </div>
                    <div className="p-0 overflow-x-auto text-[12px]">
                        <SyntaxHighlighter
                            language="sql"
                            style={vscDarkPlus}
                            customStyle={{
                                margin: 0,
                                padding: '0.75rem',
                                background: 'transparent',
                                fontSize: '12px',
                                textShadow: 'none',
                                fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace'
                            }}
                            wrapLongLines={true}
                        >
                            {payload.sqlPreview}
                        </SyntaxHighlighter>
                    </div>
                </div>
            )}

            {target && (
                <p className="text-[12px] theme-text-secondary mb-2">
                    {t(I18N_KEYS.AI.WRITE_CONFIRM.TARGET_LABEL)}:{' '}
                    <code className="font-mono theme-text-primary">{target}</code>
                </p>
            )}

            <div className="flex flex-col gap-2 mt-2">
                <button
                    type="button"
                    onClick={handleConfirm}
                    disabled={isProcessing || isLoading || payload.error}
                    className="w-full px-3 py-1.5 rounded-md text-[12px] font-medium bg-red-600 hover:bg-red-700 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {t(I18N_KEYS.AI.WRITE_CONFIRM.YES_BTN)}
                </button>
                <button
                    type="button"
                    onClick={handleCancel}
                    disabled={isProcessing || isLoading}
                    className="w-full px-3 py-1.5 rounded-md text-[12px] font-medium theme-bg-panel theme-text-secondary border theme-border hover:theme-bg-hover transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
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
