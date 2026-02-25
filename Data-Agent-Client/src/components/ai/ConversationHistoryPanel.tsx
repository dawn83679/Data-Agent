import { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { useToast } from '../../hooks/useToast';
import { conversationService } from '../../services/conversation.service';
import type { Conversation } from '../../types/conversation';
import { MessageCircle, Pencil, Trash2, ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '../../lib/utils';

const PAGE_SIZE = 20;

// Keyboard event keys
const KEYS = {
  ARROW_DOWN: 'ArrowDown',
  ARROW_UP: 'ArrowUp',
  ENTER: 'Enter',
  ESCAPE: 'Escape',
  SPACE: ' ',
} as const;

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
  if (tMs >= startOfToday) return t(I18N_KEYS.AI.TODAY);
  if (tMs >= startOfYesterday) return t(I18N_KEYS.AI.YESTERDAY);
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
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const itemRefs = useRef<Map<number, HTMLElement>>(new Map());
  const [loading, setLoading] = useState(false);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [current, setCurrent] = useState(1);
  const [total, setTotal] = useState(0);
  const [pages, setPages] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const [pendingHighlightIndex, setPendingHighlightIndex] = useState<number | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const lastScrollTimeRef = useRef(0);

  const fetchList = useCallback(
    async (page: number = 1, pendingIndex: number | null = null) => {
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
        // If pagination triggered by keyboard, apply the pending highlight after data loads
        if (pendingIndex !== null) {
          setPendingHighlightIndex(pendingIndex);
        }
      } catch (err) {
        toast.error(t(I18N_KEYS.AI.LOAD_FAILED));
      } finally {
        setLoading(false);
      }
    },
    [open, toast, t]
  );

  // When data loads and we have a pending highlight, apply it
  useEffect(() => {
    if (pendingHighlightIndex !== null && conversations.length > 0) {
      setHighlightedIndex(pendingHighlightIndex);
      setPendingHighlightIndex(null);
    }
  }, [pendingHighlightIndex, conversations.length]);

  useEffect(() => {
    if (open) {
      fetchList(1);
      // Focus search input when panel opens
      setTimeout(() => {
        searchInputRef.current?.focus();
        setHighlightedIndex(-1);
      }, 0);
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

  // Compute grouped and flatList
  const filtered = searchQuery.trim()
    ? conversations.filter((c) =>
        (c.title ?? '').toLowerCase().includes(searchQuery.trim().toLowerCase())
      )
    : conversations;
  const grouped = groupByDate(filtered, t);
  const flatList = grouped.flatMap(g => g.items);
  const highlightedConversation = flatList[highlightedIndex];

  // Scroll highlighted item into view smoothly
  useEffect(() => {
    if (highlightedIndex < 0 || !highlightedConversation) return;
    // Small delay to ensure DOM has updated before scrolling
    const timeoutId = setTimeout(() => {
      const itemElement = itemRefs.current.get(highlightedConversation.id);
      if (itemElement && scrollContainerRef.current) {
        itemElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      lastScrollTimeRef.current = Date.now();
    }, 50);
    return () => clearTimeout(timeoutId);
  }, [highlightedIndex, highlightedConversation]);

  useEffect(() => {
    if (!open || pendingHighlightIndex !== null) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      const isSearchFocused = document.activeElement === searchInputRef.current;
      const currentFlatList = grouped.flatMap(g => g.items);

      // Skip if search is focused (it handles its own arrow keys)
      if (isSearchFocused) return;

      // Arrow key navigation (when search is NOT focused)
      if (e.key === KEYS.ARROW_DOWN || e.key === KEYS.ARROW_UP) {
        e.preventDefault();
        if (e.key === KEYS.ARROW_DOWN) {
          setHighlightedIndex((prev) => {
            if (prev < 0) return 0; // First item if not selected
            const nextIndex = prev + 1;
            if (nextIndex >= currentFlatList.length && current < pages) {
              // Load next page if at end and more pages exist
              fetchList(current + 1, 0);
              return prev; // Keep current state while loading
            }
            return Math.min(nextIndex, currentFlatList.length - 1);
          });
        } else {
          setHighlightedIndex((prev) => {
            if (prev < 0) return Math.max(0, currentFlatList.length - 1);
            const nextIndex = prev - 1;
            if (nextIndex < 0 && current > 1) {
              // Load previous page if at start and previous page exists
              fetchList(current - 1, Math.max(0, currentFlatList.length - 1));
              return prev; // Keep current state while loading
            }
            return Math.max(nextIndex, 0);
          });
        }
      } else if (e.key === KEYS.ENTER && highlightedIndex >= 0) {
        e.preventDefault();
        const conversation = currentFlatList[highlightedIndex];
        if (conversation) {
          handleSelect(conversation);
        }
      } else if (e.key === KEYS.SPACE) {
        e.preventDefault();
        // If search is not focused, focus it for typing
        if (!isSearchFocused) {
          searchInputRef.current?.focus();
          setHighlightedIndex(-1);
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, grouped, highlightedIndex, pendingHighlightIndex, current, pages, fetchList]);

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
      toast.success(t(I18N_KEYS.AI.RENAME_SUCCESS));
    } catch {
      toast.error(t(I18N_KEYS.AI.LOAD_FAILED));
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
      toast.success(t(I18N_KEYS.AI.DELETE_SUCCESS));
    } catch {
      toast.error(t(I18N_KEYS.AI.LOAD_FAILED));
    } finally {
      setDeletingId(null);
    }
  };

  if (!open) return null;

  return (
    <div
        ref={panelRef}
        role="dialog"
        aria-label={t(I18N_KEYS.AI.HISTORY_TITLE)}
        className={cn(
          'absolute right-0 top-full mt-1 w-80 max-h-[70vh] flex flex-col',
          'theme-bg-panel theme-border border rounded-lg shadow-xl z-50',
          'animate-in fade-in slide-in-from-top-2 duration-200'
        )}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-2 border-b theme-border shrink-0">
          <Input
            ref={searchInputRef}
            type="text"
            placeholder={t(I18N_KEYS.COMMON.SEARCH)}
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setHighlightedIndex(-1);
            }}
            onKeyDown={(e) => {
              if (e.key === KEYS.ESCAPE) {
                e.preventDefault();
                onClose();
              } else if (e.key === KEYS.SPACE) {
                e.preventDefault();
                e.stopPropagation();
                searchInputRef.current?.blur();
                setHighlightedIndex(0);
              }
            }}
            className="h-8 text-sm"
          />
        </div>
        <div ref={scrollContainerRef} className="flex-1 overflow-y-auto min-h-0">
          {loading ? (
            <p className="text-sm theme-text-secondary py-4 text-center">
              {t(I18N_KEYS.AI.LOADING_CONVERSATIONS)}
            </p>
          ) : filtered.length === 0 ? (
            <p className="text-sm theme-text-secondary py-4 text-center">
              {t(I18N_KEYS.AI.NO_CONVERSATIONS)}
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
                        ref={(el) => {
                          if (el) itemRefs.current.set(c.id, el);
                          else itemRefs.current.delete(c.id);
                        }}
                        key={c.id}
                        className={cn(
                          'flex items-center gap-2 px-2 py-1.5 rounded-md group transition-colors',
                          'hover:theme-bg-main/80',
                          highlightedConversation?.id === c.id && 'bg-violet-600/40',
                          currentConversationId === c.id && 'theme-bg-main/50'
                        )}
                      >
                        {editingId === c.id ? (
                          <div className="flex-1 flex items-center gap-1 min-w-0">
                            <Input
                              value={editTitle}
                              onChange={(e) => setEditTitle(e.target.value)}
                              placeholder={t(I18N_KEYS.AI.RENAME_PLACEHOLDER)}
                              className="h-7 text-xs flex-1 min-w-0"
                              onKeyDown={(e) => {
                                if (e.key === 'Enter') saveRename();
                                if (e.key === 'Escape') cancelRename();
                              }}
                              autoFocus
                            />
                            <Button size="sm" variant="ghost" className="h-7 px-1.5" onClick={saveRename}>
                              {t(I18N_KEYS.CONNECTIONS.SAVE)}
                            </Button>
                            <Button size="sm" variant="ghost" className="h-7 px-1.5" onClick={cancelRename}>
                              {t(I18N_KEYS.CONNECTIONS.CANCEL)}
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
                                title={t(I18N_KEYS.AI.RENAME)}
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
                                title={t(I18N_KEYS.AI.DELETE)}
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
