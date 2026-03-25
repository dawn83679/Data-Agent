import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ConfirmActionDialog } from '../components/common/ConfirmActionDialog';
import type { ButtonProps } from '../components/ui/Button';
import { I18N_KEYS } from '../constants/i18nKeys';

interface ConfirmDialogOptions {
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  confirmVariant?: ButtonProps['variant'];
}

interface ConfirmDialogState {
  title: string;
  description: string;
  confirmLabel: string;
  cancelLabel: string;
  confirmVariant: ButtonProps['variant'];
}

export function useConfirmDialog() {
  const { t } = useTranslation();
  const [dialogState, setDialogState] = useState<ConfirmDialogState | null>(null);
  const resolverRef = useRef<((confirmed: boolean) => void) | null>(null);

  const settle = useCallback((confirmed: boolean) => {
    const resolver = resolverRef.current;
    resolverRef.current = null;
    setDialogState(null);
    resolver?.(confirmed);
  }, []);

  useEffect(() => () => {
    if (resolverRef.current) {
      resolverRef.current(false);
      resolverRef.current = null;
    }
  }, []);

  const confirm = useCallback((options: ConfirmDialogOptions): Promise<boolean> => new Promise((resolve) => {
    if (resolverRef.current) {
      resolverRef.current(false);
    }

    resolverRef.current = resolve;
    setDialogState({
      title: options.title,
      description: options.description,
      confirmLabel: options.confirmLabel ?? t(I18N_KEYS.COMMON.CONFIRM),
      cancelLabel: options.cancelLabel ?? t(I18N_KEYS.COMMON.CANCEL),
      confirmVariant: options.confirmVariant ?? 'default',
    });
  }), [t]);

  const confirmDialog = useMemo(() => (
    <ConfirmActionDialog
      open={dialogState != null}
      onOpenChange={(open) => {
        if (!open) {
          settle(false);
        }
      }}
      title={dialogState?.title ?? ''}
      description={dialogState?.description ?? ''}
      confirmLabel={dialogState?.confirmLabel ?? t(I18N_KEYS.COMMON.CONFIRM)}
      cancelLabel={dialogState?.cancelLabel ?? t(I18N_KEYS.COMMON.CANCEL)}
      confirmVariant={dialogState?.confirmVariant ?? 'default'}
      onConfirm={() => settle(true)}
    />
  ), [dialogState, settle, t]);

  return {
    confirm,
    confirmDialog,
  };
}
