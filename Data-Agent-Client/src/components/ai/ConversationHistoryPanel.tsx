import { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { useToast } from '../../hooks/useToast';
import { conversationService } from '../../services/conversation.service';
import type { Conversation } from '../../types/conversation';
import { MessageCircle, Pencil, Trash2, ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '../../lib/utils';

const PAGE_SIZE = 20;

function formatRelativeTime(iso: string): string {
  const d = new Date(iso);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m`;
  if (diffHours < 24) return `${diffHours}h`;
  if (diffDays < 7) return `${diffDays}d`;
  return d.toLocaleDateString();
}

function getGroupLabel(iso: string, t: (k: string) => string): string {
  const d = new Date(iso);
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const startOfYesterday = startOfToday - 86400000;
  const tMs = d.getTime();
  if (tMs >= startOfToday) return t('ai.today');
  if (tMs >= startOfYesterday) return t('ai.yesterday');
  const diffDays = Math.floor((startOfToday - tMs) / 86400000);
  if (diffDays < 7) return `${diffDays}d ago`;
  return d.toLocaleDateString();
}

function groupByDate(conversations: Conversation[], t: (k: string) => string): { label: string; items: Conversation[] }[] {
  const map = new Map<string, Conversation[]>();
  for (const c of conversations) {
    const label = getGroupLabel(c.updatedAt, t);
    if (!map.has(label)) map.set(label, []);
    map.get(label)!.push(c);
  }
  return Array.from(map.entries()).map(([label, items]) => ({ label, items }));
}

export interface ConversationHistoryPanelProps {
  open: boolean;
  onClose: () => void;
  onSelectConversation: (id: number) => void;
  onNewChat: () => void;
  currentConversationId: number | null;
}

export function ConversationHistoryPanel({
  open,
  onClose,
  onSelectConversation,
  onNewChat,
  currentConversationId,
}: ConversationHistoryPanelProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const panelRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(false);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [current, setCurrent] = useState(1);
  const [total, setTotal] = useState(0);
  const [pages, setPages] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const fetchList = useCallback(
    async (page: number = 1) => {
      if (!open) return;
      setLoading(true);
      try {
        const res = await conversationService.getList({
          current: page,
          size: PAGE_SIZE,
        });
        setConversations(res.records);
        setCurrent(res.current);
        setTotal(res.total);
        setPages(res.pages);
      } catch (err) {
        toast.error(t('ai.load_failed'));
      } finally {
        setLoading(false);
      }
    },
    [open, toast, t]
  );

  useEffect(() => {
    if (open) {
      fetchList(1);
    }
  }, [open, fetchList]);

  useEffect(() => {
    if (!open) return;
    const onDocClick = () => onClose();
    const timer = setTimeout(() => document.addEventListener('click', onDocClick), 0);
    return () => {
      clearTimeout(timer);
      document.removeEventListener('click', onDocClick);
    };
  }, [open, onClose]);

  const handleSelect = (c: Conversation) => {
    onSelectConversation(c.id);
    onClose();
  };

  const startRename = (c: Conversation) => {
    setEditingId(c.id);
    setEditTitle(c.title ?? '');
  };

  const saveRename = async () => {
    if (editingId == null || !editTitle.trim()) {
      setEditingId(null);
      return;
    }
    try {
      await conversationService.updateTitle(editingId, { title: editTitle.trim() });
      setConversations((prev) =>
        prev.map((item) =>
          item.id === editingId ? { ...item, title: editTitle.trim() } : item
        )
      );
      setEditingId(null);
      toast.success(t('ai.rename_success'));
    } catch {
      toast.error(t('ai.load_failed'));
    }
  };

  const cancelRename = () => {
    setEditingId(null);
    setEditTitle('');
  };

  const handleDelete = async (id: number) => {
    if (deletingId !== null) return;
    setDeletingId(id);
    try {
      await conversationService.delete(id);
      setConversations((prev) => prev.filter((c) => c.id !== id));
      setTotal((prev) => Math.max(0, prev - 1));
      if (currentConversationId === id) {
        onNewChat();
      }
      toast.success(t('ai.delete_success'));
    } catch {
      toast.error(t('ai.load_failed'));
    } finally {
      setDeletingId(null);
    }
  };

  const filtered = searchQuery.trim()
    ? conversations.filter((c) =>
        (c.title ?? '').toLowerCase().includes(searchQuery.trim().toLowerCase())
      )
    : conversations;
  const grouped = groupByDate(filtered, t);

  if (!open) return null;

  return (
    <div
        ref={panelRef}
        role="dialog"
        aria-label={t('ai.history_title')}
        className={cn(
          'absolute right-0 top-full mt-1 w-80 max-h-[70vh] flex flex-col',
          'theme-bg-panel theme-border border rounded-lg shadow-xl z-50',
          'animate-in fade-in slide-in-from-top-2 duration-200'
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-2 border-b theme-border shrink-0">
          <Input
            type="text"
            placeholder={t('common.search')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-8 text-sm"
          />
        </div>
        <div className="flex-1 overflow-y-auto min-h-0">
          {loading ? (
            <p className="text-sm theme-text-secondary py-4 text-center">
              {t('ai.loading_conversations')}
            </p>
          ) : filtered.length === 0 ? (
            <p className="text-sm theme-text-secondary py-4 text-center">
              {t('ai.no_conversations')}
            </p>
          ) : (
            <div className="py-1">
              {grouped.map(({ label, items }) => (
                <div key={label} className="mb-2">
                  <p className="px-2 py-1 text-[11px] font-medium theme-text-secondary uppercase tracking-wider">
                    {label}
                  </p>
                  <ul className="space-y-0.5">
                    {items.map((c) => (
                      <li
                        key={c.id}
                        className={cn(
                          'flex items-center gap-2 px-2 py-1.5 rounded-md group',
                          'hover:theme-bg-main/80',
                          currentConversationId === c.id && 'theme-bg-main/50'
                        )}
                      >
                        {editingId === c.id ? (
                          <div className="flex-1 flex items-center gap-1 min-w-0">
                            <Input
                              value={editTitle}
                              onChange={(e) => setEditTitle(e.target.value)}
                              placeholder={t('ai.rename_placeholder')}
                              className="h-7 text-xs flex-1 min-w-0"
                              onKeyDown={(e) => {
                                if (e.key === 'Enter') saveRename();
                                if (e.key === 'Escape') cancelRename();
                              }}
                              autoFocus
                            />
                            <Button size="sm" variant="ghost" className="h-7 px-1.5" onClick={saveRename}>
                              {t('connections.save')}
                            </Button>
                            <Button size="sm" variant="ghost" className="h-7 px-1.5" onClick={cancelRename}>
                              {t('connections.cancel')}
                            </Button>
                          </div>
                        ) : (
                          <>
                            <MessageCircle className="w-3.5 h-3.5 shrink-0 theme-text-secondary" />
                            <button
                              type="button"
                              className="flex-1 text-left min-w-0 truncate"
                              onClick={() => handleSelect(c)}
                            >
                              <span className="text-sm theme-text-primary truncate block">
                                {c.title || `Conversation ${c.id}`}
                              </span>
                            </button>
                            <span className="text-[11px] theme-text-secondary shrink-0">
                              {formatRelativeTime(c.updatedAt)}
                            </span>
                            <div className="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
                              <button
                                type="button"
                                className="p-1 rounded theme-text-secondary hover:theme-text-primary"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  startRename(c);
                                }}
                                title={t('ai.rename')}
                              >
                                <Pencil className="w-3 h-3" />
                              </button>
                              <button
                                type="button"
                                className="p-1 rounded theme-text-secondary hover:text-red-500"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDelete(c.id);
                                }}
                                disabled={deletingId === c.id}
                                title={t('ai.delete')}
                              >
                                <Trash2 className="w-3 h-3" />
                              </button>
                            </div>
                          </>
                        )}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
              {pages > 1 && (
                <div className="flex items-center justify-center gap-1 py-2 border-t theme-border mt-1">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7"
                    disabled={current <= 1}
                    onClick={() => fetchList(current - 1)}
                  >
                    <ChevronLeft className="w-3.5 h-3.5" />
                  </Button>
                  <span className="text-[11px] theme-text-secondary px-1">
                    {current}/{pages} ({total})
                  </span>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7"
                    disabled={current >= pages}
                    onClick={() => fetchList(current + 1)}
                  >
                    <ChevronRight className="w-3.5 h-3.5" />
                  </Button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
  );
}
