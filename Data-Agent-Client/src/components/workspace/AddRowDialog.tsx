import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../ui/Dialog';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { columnService, type ColumnMetadata } from '../../services/column.service';
import { I18N_KEYS } from '../../constants/i18nKeys';

interface AddRowDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tableName: string;
  connectionId: string;
  catalog?: string;
  schema?: string;
  displayName: string;
  onSuccess: () => void;
  executeInsert: (sql: string) => Promise<{ success: boolean; errorMessage?: string }>;
}

function escapeSqlString(val: string): string {
  return "'" + String(val).replace(/'/g, "''") + "'";
}

function quoteIdentifier(name: string): string {
  return '`' + String(name).replace(/`/g, '``') + '`';
}

export function AddRowDialog({
  open,
  onOpenChange,
  tableName,
  connectionId,
  catalog,
  schema,
  displayName,
  onSuccess,
  executeInsert,
}: AddRowDialogProps) {
  const { t } = useTranslation();
  const [columns, setColumns] = useState<ColumnMetadata[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});

  useEffect(() => {
    if (open) {
      setValues({});
      setError(null);
      setLoading(true);
      columnService
        .listColumns(connectionId, tableName, catalog, schema)
        .then((cols) => setColumns(cols || []))
        .catch((err) => setError((err as Error).message))
        .finally(() => setLoading(false));
    }
  }, [open, connectionId, tableName, catalog, schema]);

  const editableColumns = columns.filter((c) => !c.isAutoIncrement);

  const handleChange = (colName: string, val: string) => {
    setValues((prev) => ({ ...prev, [colName]: val }));
  };

  const handleSubmit = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const cols: string[] = [];
      const vals: string[] = [];
      for (const col of editableColumns) {
        const v = (values[col.name] ?? '').trim();
        const nullable = col.nullable ?? true;
        if (v === '' || v.toUpperCase() === 'NULL') {
          if (!nullable) {
            setError(`Column ${col.name} is required`);
            return;
          }
          cols.push(quoteIdentifier(col.name));
          vals.push('NULL');
        } else {
          cols.push(quoteIdentifier(col.name));
          const typeLower = (col.typeName || '').toLowerCase();
          const isNumeric =
            typeLower.includes('int') ||
            typeLower.includes('decimal') ||
            typeLower.includes('numeric') ||
            typeLower.includes('float') ||
            typeLower.includes('double') ||
            typeLower.includes('bit');
          if (isNumeric && /^-?\d*\.?\d+$/.test(v)) {
            vals.push(v);
          } else {
            vals.push(escapeSqlString(v));
          }
        }
      }
      if (cols.length === 0) {
        setError('At least one column value is required');
        return;
      }
      const catalogOrSchema = catalog || schema || '';
      const tablePart = catalogOrSchema ? `${quoteIdentifier(catalogOrSchema)}.${quoteIdentifier(tableName)}` : quoteIdentifier(tableName);
      const sql = `INSERT INTO ${tablePart} (${cols.join(', ')}) VALUES (${vals.join(', ')})`;
      const result = await executeInsert(sql);
      if (result.success) {
        onSuccess();
        onOpenChange(false);
      } else {
        setError(result.errorMessage || 'Insert failed');
      }
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[85vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>{t(I18N_KEYS.EXPLORER.ADD_ROW)}</DialogTitle>
          <DialogDescription className="font-mono text-xs">{displayName}</DialogDescription>
        </DialogHeader>
        <div className="flex-1 overflow-auto min-h-0">
          {loading && (
            <div className="py-8 text-center theme-text-secondary text-sm">{t(I18N_KEYS.COMMON.LOADING)}...</div>
          )}
          {!loading && editableColumns.length === 0 && !error && (
            <div className="py-8 text-center theme-text-secondary text-sm">{t(I18N_KEYS.EXPLORER.NO_DATA)}</div>
          )}
          {!loading && editableColumns.length > 0 && (
            <div className="space-y-3 py-2">
              {editableColumns.map((col) => (
                <div key={col.name} className="flex flex-col gap-1">
                  <label className="text-xs font-medium theme-text-secondary">
                    {col.name}
                    {!col.nullable && <span className="text-destructive ml-0.5">*</span>}
                    {col.typeName && (
                      <span className="font-normal text-[10px] ml-1 opacity-70">({col.typeName})</span>
                    )}
                  </label>
                  <Input
                    value={values[col.name] ?? ''}
                    onChange={(e) => handleChange(col.name, e.target.value)}
                    placeholder={col.nullable ? 'NULL' : ''}
                    className="h-8 text-xs font-mono"
                  />
                </div>
              ))}
            </div>
          )}
          {error && (
            <div className="p-3 mt-2 rounded-md bg-destructive/10 text-destructive text-sm">{error}</div>
          )}
        </div>
        <div className="flex justify-end gap-2 pt-4 border-t theme-border shrink-0">
          <Button variant="outline" size="sm" onClick={() => onOpenChange(false)} disabled={submitting}>
            {t(I18N_KEYS.EXPLORER.CLOSE)}
          </Button>
          <Button size="sm" onClick={handleSubmit} disabled={submitting || loading || editableColumns.length === 0}>
            {submitting ? t(I18N_KEYS.COMMON.LOADING) + '...' : t(I18N_KEYS.EXPLORER.INSERT_ROW)}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
