import { useState, useEffect } from 'react';
import { Copy, Check, Loader2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../ui/Dialog';
import { Button } from '../ui/Button';
import { tableService } from '../../services/table.service';

interface TableDdlDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  connectionId: string;
  tableName: string;
  catalog?: string;
  schema?: string;
}

export function TableDdlDialog({
  open,
  onOpenChange,
  connectionId,
  tableName,
  catalog,
  schema,
}: TableDdlDialogProps) {
  const { t } = useTranslation();
  const [ddl, setDdl] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (open) {
      loadDdl();
    } else {
      // Reset state when dialog closes
      setDdl('');
      setError(null);
      setCopied(false);
    }
  }, [open, connectionId, tableName, catalog, schema]);

  const loadDdl = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await tableService.getTableDdl(connectionId, tableName, catalog, schema);
      setDdl(result);
    } catch (err: any) {
      console.error('Failed to load table DDL:', err);
      setError(err.message || t('explorer.load_ddl_failed'));
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(ddl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy DDL:', err);
    }
  };

  const displayName = [catalog, schema, tableName].filter(Boolean).join('.');

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>{t('explorer.table_ddl')}</DialogTitle>
          <DialogDescription className="font-mono text-xs">
            {displayName}
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 overflow-hidden flex flex-col gap-2">
          {loading && (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-6 h-6 animate-spin theme-text-secondary" />
            </div>
          )}

          {error && (
            <div className="p-4 bg-destructive/10 text-destructive rounded-md text-sm">
              {error}
            </div>
          )}

          {!loading && !error && ddl && (
            <>
              <div className="flex items-center justify-between">
                <span className="text-xs theme-text-secondary">
                  {ddl.split('\n').length} lines
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleCopy}
                  className="h-7"
                >
                  {copied ? (
                    <>
                      <Check className="w-3 h-3 mr-1" />
                      {t('explorer.ddl_copied')}
                    </>
                  ) : (
                    <>
                      <Copy className="w-3 h-3 mr-1" />
                      {t('explorer.copy_ddl')}
                    </>
                  )}
                </Button>
              </div>

              <div className="flex-1 overflow-auto border theme-border rounded-md">
                <pre className="p-4 text-xs font-mono whitespace-pre-wrap theme-text-primary">
                  {ddl}
                </pre>
              </div>
            </>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
