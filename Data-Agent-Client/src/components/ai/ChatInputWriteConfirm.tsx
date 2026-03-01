import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useWriteConfirmModalStore } from '../../store/writeConfirmModalStore';
import { I18N_KEYS } from '../../constants/i18nKeys';

interface ChatInputWriteConfirmProps {
  conversationId: number | null;
}

/** Inline write-confirmation panel, shown above the chat input area. Same layout as ChatInputQuestion. */
export function ChatInputWriteConfirm({ conversationId }: ChatInputWriteConfirmProps) {
  const { t } = useTranslation();
  const { isOpen, conversationId: confirmConversationId, payload, onConfirm, onCancel, close } =
    useWriteConfirmModalStore();

  const isVisible =
    isOpen && payload != null && conversationId !== null && confirmConversationId === conversationId;

  const handleConfirm = useCallback(() => {
    onConfirm?.();
    close();
  }, [onConfirm, close]);

  const handleCancel = useCallback(() => {
    onCancel?.();
    close();
  }, [onCancel, close]);

  if (!isVisible || !payload) return null;

  const target = payload.schemaName
    ? `${payload.databaseName}.${payload.schemaName}`
    : payload.databaseName;

  return (
    <div className="p-2 theme-bg-panel border-t theme-border shrink-0">
      <div className="rounded-lg border theme-border theme-bg-main p-4">
        <div className="px-1 pt-0.5 pb-1">
          {/* Label */}
          <p className="text-[11px] font-medium theme-text-muted uppercase tracking-wide mb-2">
            {t(I18N_KEYS.AI.WRITE_CONFIRM.LABEL)}
          </p>

          {/* Explanation */}
          {payload.explanation && (
            <p className="theme-text-primary text-[13px] mb-3 whitespace-pre-wrap">
              {payload.explanation}
            </p>
          )}

          {/* SQL preview */}
          <div className="mb-3">
            <p className="text-[11px] theme-text-muted mb-1">
              {t(I18N_KEYS.AI.WRITE_CONFIRM.SQL_PREVIEW_LABEL)}
            </p>
            <pre className="text-[12px] font-mono theme-bg-panel rounded border theme-border px-3 py-2 overflow-x-auto whitespace-pre-wrap break-all theme-text-primary">
              {payload.sqlPreview}
            </pre>
          </div>

          {/* Target database */}
          <p className="text-[12px] theme-text-secondary mb-3">
            {t(I18N_KEYS.AI.WRITE_CONFIRM.TARGET_LABEL)}:{' '}
            <code className="font-mono theme-text-primary">{target}</code>
          </p>

          {/* Warning */}
          <p className="text-[12px] text-amber-600 dark:text-amber-400 mb-3">
            ⚠ {t(I18N_KEYS.AI.WRITE_CONFIRM.WARNING)}
          </p>

          {/* Buttons */}
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleConfirm}
              className="flex-1 px-3 py-1.5 rounded-md text-[12px] font-medium bg-red-600 hover:bg-red-700 text-white transition-colors"
            >
              {t(I18N_KEYS.AI.WRITE_CONFIRM.CONFIRM_BTN)}
            </button>
            <button
              type="button"
              onClick={handleCancel}
              className="px-3 py-1.5 rounded-md text-[12px] font-medium theme-bg-panel theme-text-secondary border theme-border hover:theme-bg-hover transition-colors"
            >
              {t(I18N_KEYS.AI.WRITE_CONFIRM.CANCEL_BTN)}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
