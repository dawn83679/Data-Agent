import { useState, useEffect } from 'react';
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
import { databaseService } from '../../services/database.service';

interface CreateDatabaseDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  connectionId: number;
  onSuccess: (connectionId: number, databaseName: string) => void;
}

export function CreateDatabaseDialog({
  open,
  onOpenChange,
  connectionId,
  onSuccess,
}: CreateDatabaseDialogProps) {
  const { t } = useTranslation();
  const [databaseName, setDatabaseName] = useState('');
  const [charset, setCharset] = useState('');
  const [collation, setCollation] = useState('');
  const [charsets, setCharsets] = useState<string[]>([]);
  const [collations, setCollations] = useState<string[]>([]);
  const [isLoadingCharsets, setIsLoadingCharsets] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState('');
  const [nameExists, setNameExists] = useState(false);

  // Load character sets when dialog opens
  useEffect(() => {
    if (open && connectionId) {
      loadCharacterSets();
    }
  }, [open, connectionId]);

  // Load collations when charset changes
  useEffect(() => {
    if (charset && connectionId) {
      loadCollations(charset);
    } else {
      setCollations([]);
      setCollation('');
    }
  }, [charset, connectionId]);

  // Reset form when dialog closes
  useEffect(() => {
    if (!open) {
      setDatabaseName('');
      setCharset('');
      setCollation('');
      setCharsets([]);
      setCollations([]);
      setError('');
      setNameExists(false);
    }
  }, [open]);

  const loadCharacterSets = async () => {
    setIsLoadingCharsets(true);
    try {
      const charsets = await databaseService.getCharacterSets(String(connectionId));
      setCharsets(charsets);

      // Set default charset (usually utf8mb4 is the most commonly used)
      const defaultCharset = charsets.includes('utf8mb4') ? 'utf8mb4' : charsets[0];
      if (defaultCharset) {
        setCharset(defaultCharset);
      }
    } catch (err) {
      console.error('Failed to load character sets:', err);
      setError(t('explorer.load_charsets_failed'));
    } finally {
      setIsLoadingCharsets(false);
    }
  };

  const loadCollations = async (selectedCharset: string) => {
    try {
      const collations = await databaseService.getCollations(String(connectionId), selectedCharset);
      setCollations(collations);

      // Set default collation (usually the first one or one ending with _general_ci or _unicode_ci)
      if (collations.length > 0) {
        const defaultCollation = collations.find(c => c.endsWith('_general_ci')) ||
          collations.find(c => c.endsWith('_unicode_ci')) ||
          collations[0];
        setCollation(defaultCollation);
      }
    } catch (err) {
      console.error('Failed to load collations:', err);
    }
  };

  const checkDatabaseName = async (name: string) => {
    if (!name.trim()) {
      setNameExists(false);
      return;
    }
    try {
      const exists = await databaseService.databaseExists(String(connectionId), name);
      setNameExists(exists);
    } catch (err) {
      console.error('Failed to check database existence:', err);
    }
  };

  const handleDatabaseNameChange = (value: string) => {
    setDatabaseName(value);
    setError('');
    // Debounce check
    const timeoutId = setTimeout(() => checkDatabaseName(value), 300);
    return () => clearTimeout(timeoutId);
  };

  const handleCreate = async () => {
    if (!databaseName.trim()) {
      setError(t('explorer.database_name_required'));
      return;
    }

    if (nameExists) {
      setError(t('explorer.database_name_exists'));
      return;
    }

    if (!charset) {
      setError(t('explorer.charset_required'));
      return;
    }

    setIsCreating(true);
    setError('');

    try {
      await databaseService.createDatabase(
        String(connectionId),
        databaseName.trim(),
        charset,
        collation || undefined
      );
      onSuccess(connectionId, databaseName.trim());
      onOpenChange(false);
    } catch (err: any) {
      console.error('Failed to create database:', err);
      setError(err.response?.data?.message || t('explorer.create_database_failed'));
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[450px]">
        <DialogHeader>
          <DialogTitle>{t('explorer.create_database')}</DialogTitle>
          <DialogDescription>
            {t('explorer.create_database_desc')}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-4">
          {/* Database Name */}
          <div className="grid gap-2">
            <label htmlFor="dbName" className="text-sm font-medium">
              {t('explorer.database_name')}
            </label>
            <input
              id="dbName"
              type="text"
              value={databaseName}
              onChange={(e) => handleDatabaseNameChange(e.target.value)}
              placeholder={t('explorer.database_name_placeholder')}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              autoComplete="off"
            />
            {nameExists && (
              <p className="text-xs text-red-500">{t('explorer.database_name_exists')}</p>
            )}
          </div>

          {/* Character Set */}
          <div className="grid gap-2">
            <label htmlFor="charset" className="text-sm font-medium">
              {t('explorer.charset')}
            </label>
            <select
              id="charset"
              value={charset}
              onChange={(e) => setCharset(e.target.value)}
              disabled={isLoadingCharsets}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              {isLoadingCharsets ? (
                <option value="">{t('explorer.loading')}</option>
              ) : (
                charsets.map((cs) => (
                  <option key={cs} value={cs}>
                    {cs}
                  </option>
                ))
              )}
            </select>
          </div>

          {/* Collation */}
          <div className="grid gap-2">
            <label htmlFor="collation" className="text-sm font-medium">
              {t('explorer.collation')}
            </label>
            <select
              id="collation"
              value={collation}
              onChange={(e) => setCollation(e.target.value)}
              disabled={!charset || collations.length === 0}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              {!charset ? (
                <option value="">{t('explorer.select_charset_first')}</option>
              ) : collations.length === 0 ? (
                <option value="">{t('explorer.loading')}</option>
              ) : (
                collations.map((col) => (
                  <option key={col} value={col}>
                    {col}
                  </option>
                ))
              )}
            </select>
          </div>

          {/* Error Message */}
          {error && (
            <p className="text-xs text-red-500">{error}</p>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isCreating}>
            {t('connections.cancel')}
          </Button>
          <Button
            onClick={handleCreate}
            disabled={isCreating || !databaseName.trim() || nameExists}
          >
            {isCreating ? t('explorer.creating') : t('explorer.create')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
