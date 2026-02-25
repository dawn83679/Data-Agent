import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/Dialog';
import { Button } from '../ui/Button';
import { DELETE_DIALOG_CONFIG } from '../../constants/deleteConfig';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { ExplorerNodeType } from '../../constants/explorer';

interface DeleteEntityDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  entityName: string;
  entityType: ExplorerNodeType;
  onConfirm: () => void;
  isPending: boolean;
  itemCount?: number; // For folder deletion
}

export function DeleteEntityDialog({
  open,
  onOpenChange,
  entityName,
  entityType,
  onConfirm,
  isPending,
  itemCount,
}: DeleteEntityDialogProps) {
  const { t } = useTranslation();

  const config = DELETE_DIALOG_CONFIG[entityType];
  if (!config) return null;

  const handleConfirm = () => {
    onConfirm();
    onOpenChange(false);
  };

  const title = t(config.titleKey);
  const message = itemCount !== undefined
    ? t(config.messageKey, { name: entityName, count: itemCount })
    : t(config.messageKey, { name: entityName });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[400px]">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{message}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t(I18N_KEYS.CONNECTIONS.CANCEL)}
          </Button>
          <Button
            variant="destructive"
            disabled={isPending}
            onClick={handleConfirm}
          >
            {isPending ? t(I18N_KEYS.CONNECTIONS.DELETING) : t(I18N_KEYS.CONNECTIONS.DELETE)}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
