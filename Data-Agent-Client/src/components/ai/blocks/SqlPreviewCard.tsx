import { useState, type ReactNode } from 'react';
import { Check, Copy } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { COPY_FEEDBACK_SHORT_MS } from '../../../constants/timing';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { SqlCodeBlock } from '../../common/SqlCodeBlock';

interface SqlPreviewCardProps {
  headerStart?: ReactNode;
  sql: string;
  language?: string;
  wrapLongLines?: boolean;
}

export function SqlPreviewCard({
  headerStart,
  sql,
  language = 'sql',
  wrapLongLines = true,
}: SqlPreviewCardProps) {
  const { t } = useTranslation();
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(sql);
      setCopied(true);
      setTimeout(() => setCopied(false), COPY_FEEDBACK_SHORT_MS);
    } catch {
      // Ignore clipboard failures to avoid noisy UX inside chat blocks.
    }
  };

  return (
    <div className="overflow-hidden rounded border theme-border theme-bg-main">
      <div className="flex items-center justify-between gap-2 border-b theme-border theme-bg-panel px-2 py-1 text-[10px]">
        <div className="min-w-0 flex-1 theme-text-secondary">
          {headerStart}
        </div>
        <button
          type="button"
          onClick={handleCopy}
          className="inline-flex items-center gap-1 rounded border theme-border px-2 py-1 theme-text-secondary hover:bg-black/5 dark:hover:bg-white/5"
          title={t(I18N_KEYS.EXPLORER.COPY_DDL)}
        >
          {copied ? (
            <>
              <Check className="h-3.5 w-3.5 text-green-500" />
              <span>{t(I18N_KEYS.EXPLORER.DDL_COPIED)}</span>
            </>
          ) : (
            <>
              <Copy className="h-3.5 w-3.5" />
              <span>{t(I18N_KEYS.EXPLORER.COPY_DDL)}</span>
            </>
          )}
        </button>
      </div>
      <SqlCodeBlock
        variant="compact"
        sql={sql}
        language={language}
        wrapLongLines={wrapLongLines}
      />
    </div>
  );
}
