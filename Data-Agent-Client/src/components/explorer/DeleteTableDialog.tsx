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

interface DeleteTableDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tableName: string;
  onConfirm: () => void;
  isPending: boolean;
  title?: string;
  confirmMessage?: string;
}

export function DeleteTableDialog({
  open,
  onOpenChange,
  tableName,
  onConfirm,
  isPending,
  title,
  confirmMessage,
}: DeleteTableDialogProps) {
  const { t } = useTranslation();

  const handleConfirm = () => {
    onConfirm();
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[400px]">
        <DialogHeader>
          <DialogTitle>{title || t('explorer.delete_table')}</DialogTitle>
          <DialogDescription>
            {confirmMessage || t('explorer.delete_table_confirm', { name: tableName })}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t('connections.cancel')}
          </Button>
          <Button
            variant="destructive"
            disabled={isPending}
            onClick={handleConfirm}
          >
            {isPending ? t('connections.deleting') : t('connections.delete')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
