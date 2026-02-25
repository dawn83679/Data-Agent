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
import { I18N_KEYS } from '../../constants/i18nKeys';

interface DeleteConnectionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  connectionId: number | null;
  onConfirm: (id: number) => void;
  isPending: boolean;
}

export function DeleteConnectionDialog({
  open,
  onOpenChange,
  connectionId,
  onConfirm,
  isPending,
}: DeleteConnectionDialogProps) {
  const { t } = useTranslation();

  const handleConfirm = () => {
    if (connectionId != null) {
      onConfirm(connectionId);
      onOpenChange(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[400px]">
        <DialogHeader>
          <DialogTitle>{t(I18N_KEYS.CONNECTIONS.DELETE_CONFIRM_TITLE)}</DialogTitle>
          <DialogDescription>{t(I18N_KEYS.CONNECTIONS.DELETE_CONFIRM_DESC)}</DialogDescription>
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
            {isPending ? t(I18N_KEYS.CONNECTIONS.SAVING) : t(I18N_KEYS.CONNECTIONS.DELETE)}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
