import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ShieldCheck, Trash2 } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../ui/Dialog';
import { permissionService } from '../../../services/permission.service';
import {
  permissionGrantPresetLabel,
  permissionRuleLocation,
  permissionRuleSummary,
  permissionScopeLabel,
} from '../../../lib/permissionDisplay';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import type { PermissionRule } from '../../../types/permission';
import { cn } from '../../../lib/utils';

interface PermissionRuleDialogProps {
  open: boolean;
  onClose: () => void;
  conversationId: number | null;
}

export function PermissionRuleDialog({ open, onClose, conversationId }: PermissionRuleDialogProps) {
  const { t } = useTranslation();
  const [items, setItems] = useState<PermissionRule[]>([]);
  const [loading, setLoading] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(0);
  const [busyId, setBusyId] = useState<number | null>(null);
  const itemRefs = useRef<Array<HTMLButtonElement | null>>([]);

  const reload = async () => {
    setLoading(true);
    try {
      const list = await permissionService.listRules(conversationId);
      setItems(list);
      setHighlightedIndex(0);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!open) return;
    void reload();
  }, [open, conversationId]);

  useEffect(() => {
    if (!open) return;
    itemRefs.current[highlightedIndex]?.focus();
  }, [open, highlightedIndex, items.length]);

  const toggleHighlighted = async () => {
    const item = items[highlightedIndex];
    if (!item || busyId != null) return;
    setBusyId(item.id);
    try {
      await permissionService.setRuleEnabled(item.id, !item.enabled);
      await reload();
    } finally {
      setBusyId(null);
    }
  };

  const deleteHighlighted = async () => {
    const item = items[highlightedIndex];
    if (!item || busyId != null) return;
    setBusyId(item.id);
    try {
      await permissionService.deleteRule(item.id);
      await reload();
    } finally {
      setBusyId(null);
    }
  };

  const handleKeyDown = async (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      onClose();
      return;
    }
    if (items.length === 0) return;
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setHighlightedIndex((prev) => (prev + 1) % items.length);
      return;
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault();
      setHighlightedIndex((prev) => (prev - 1 + items.length) % items.length);
      return;
    }
    if (event.key === ' ' && event.shiftKey) {
      event.preventDefault();
      await deleteHighlighted();
      return;
    }
    if (event.key === ' ') {
      event.preventDefault();
      await toggleHighlighted();
    }
  };

  const rows = useMemo(
    () =>
      items.map((item) => ({
        ...item,
        scopeLabel: permissionScopeLabel(t, item.scopeType),
        grantPresetLabel: permissionGrantPresetLabel(t, item.grantPreset),
        summary: permissionRuleSummary(t, item),
        location: permissionRuleLocation(t, item),
      })),
    [items, t]
  );

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="max-w-2xl p-0 overflow-hidden" onKeyDown={handleKeyDown}>
        <DialogHeader className="px-5 py-4 border-b theme-border">
          <DialogTitle className="flex items-center gap-2 text-base">
            <ShieldCheck className="w-4 h-4" />
            {t(I18N_KEYS.AI.PERMISSION.TITLE)}
          </DialogTitle>
        </DialogHeader>
        <div className="px-5 py-3 text-[12px] theme-text-secondary border-b theme-border">
          {t(I18N_KEYS.AI.PERMISSION.SHORTCUT_HINT)}
        </div>
        <div className="max-h-[420px] overflow-y-auto p-3 space-y-2">
          {loading ? (
            <div className="px-3 py-6 text-sm theme-text-secondary">{t(I18N_KEYS.AI.PERMISSION.LOADING)}</div>
          ) : rows.length === 0 ? (
            <div className="px-3 py-6 text-sm theme-text-secondary">{t(I18N_KEYS.AI.PERMISSION.EMPTY)}</div>
          ) : (
            rows.map((item, index) => (
              <button
                key={item.id}
                ref={(node) => {
                  itemRefs.current[index] = node;
                }}
                type="button"
                onMouseEnter={() => setHighlightedIndex(index)}
                onClick={() => setHighlightedIndex(index)}
                className={cn(
                  'w-full rounded-lg border px-3 py-3 text-left transition-colors outline-none',
                  index === highlightedIndex
                    ? 'border-emerald-500 bg-emerald-500/10'
                    : 'theme-border theme-bg-panel hover:bg-black/5 dark:hover:bg-white/5'
                )}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 text-sm font-medium theme-text-primary">
                      <span>{item.connectionName || `#${item.connectionId}`}</span>
                      <span className="rounded-full px-2 py-0.5 text-[10px] border theme-border theme-text-secondary">
                        {item.scopeLabel}
                      </span>
                    </div>
                    <div className="mt-1 text-[13px] theme-text-primary">{item.summary}</div>
                    <div className="mt-1 text-[12px] theme-text-secondary">
                      {item.grantPresetLabel} · {item.location}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <span className={cn('text-[11px] font-medium', item.enabled ? 'text-emerald-600' : 'theme-text-secondary')}>
                      {item.enabled ? t(I18N_KEYS.AI.PERMISSION.ENABLED) : t(I18N_KEYS.AI.PERMISSION.DISABLED)}
                    </span>
                    <Trash2 className="w-3.5 h-3.5 opacity-60" />
                  </div>
                </div>
              </button>
            ))
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
