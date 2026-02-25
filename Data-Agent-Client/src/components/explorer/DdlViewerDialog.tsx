import { useState, useEffect } from 'react';
import { Copy, Check, Loader2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useTheme } from '../../hooks/useTheme';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../ui/Dialog';
import { DdlViewerConfig } from '../../constants/explorer';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { Button } from '../ui/Button';

export interface DdlViewerDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  displayName: string;
  loadDdl: () => Promise<string>;
}

export function DdlViewerDialog({
  open,
  onOpenChange,
  title,
  displayName,
  loadDdl,
}: DdlViewerDialogProps) {
  const { t } = useTranslation();
  const { theme } = useTheme();
  const [ddl, setDdl] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const syntaxTheme = theme === 'dark' ? oneDark : oneLight;
  const syntaxBg = theme === 'dark' ? '#282c34' : '#fafafa';

  useEffect(() => {
    if (open) {
      loadDdlContent();
    } else {
      setDdl('');
      setError(null);
      setCopied(false);
    }
  }, [open]);

  const loadDdlContent = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await loadDdl();
      setDdl(result);
    } catch (err: unknown) {
      console.error('Failed to load DDL:', err);
      setError((err as Error).message || t(I18N_KEYS.EXPLORER.LOAD_DDL_FAILED));
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(ddl);
      setCopied(true);
      setTimeout(() => setCopied(false), DdlViewerConfig.COPY_FEEDBACK_MS);
    } catch (err) {
      console.error('Failed to copy DDL:', err);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
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
                      {t(I18N_KEYS.EXPLORER.DDL_COPIED)}
                    </>
                  ) : (
                    <>
                      <Copy className="w-3 h-3 mr-1" />
                      {t(I18N_KEYS.EXPLORER.COPY_DDL)}
                    </>
                  )}
                </Button>
              </div>

              <div className="flex-1 overflow-auto border theme-border rounded-md">
                <div className="text-[11px] max-h-[50vh] overflow-auto">
                  <SyntaxHighlighter
                    language={DdlViewerConfig.SYNTAX_LANGUAGE}
                    style={syntaxTheme}
                    showLineNumbers={false}
                    customStyle={{
                      margin: 0,
                      padding: '1rem',
                      fontSize: '11px',
                      lineHeight: 1.5,
                      background: syntaxBg,
                      minHeight: '100%',
                    }}
                    codeTagProps={{ style: { fontFamily: 'inherit' } }}
                    PreTag="div"
                  >
                    {ddl}
                  </SyntaxHighlighter>
                </div>
              </div>
            </>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
