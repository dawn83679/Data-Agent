import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { History, Plus, X } from 'lucide-react';
import { ConversationHistoryPanel } from './ConversationHistoryPanel';
import { cn } from '../../lib/utils';
import type { ConversationTabSummary } from '../../hooks/useConversationRuntime';
import type { Conversation } from '../../types/conversation';

export interface AIAssistantHeaderProps {
  title: string;
  historyAriaLabel: string;
  accessToken: boolean;
  isHistoryOpen: boolean;
  setIsHistoryOpen: React.Dispatch<React.SetStateAction<boolean>>;
  currentConversationId: number | null;
  conversationTabs: ConversationTabSummary[];
  onSelectTab: (id: number | null) => void;
  onCloseTab: (id: number | null) => void;
  onSelectConversation: (conversation: Conversation) => void;
  onNewChat: () => void;
  onClosePanel?: () => void;
}

function normalizeTabTitle(raw: string): string {
  return raw.replace(/\s+/g, ' ').trim();
}

function formatTabTitle(tab: ConversationTabSummary): { display: string; full: string } {
  const candidate = tab.titleOverride
    ? normalizeTabTitle(tab.titleOverride)
    : tab.titleCandidate
      ? normalizeTabTitle(tab.titleCandidate)
      : '';
  const fallback = tab.id == null || tab.id <= 0 ? 'New chat' : `Conversation ${tab.id}`;
  const full = candidate || fallback;
  const maxLen = 28;
  const display = full.length > maxLen ? `${full.slice(0, maxLen)}…` : full;
  return { display, full };
}

export function AIAssistantHeader({
  title: _title,
  historyAriaLabel,
  accessToken,
  isHistoryOpen,
  setIsHistoryOpen,
  currentConversationId,
  conversationTabs,
  onSelectTab,
  onCloseTab,
  onSelectConversation,
  onNewChat,
  onClosePanel,
}: AIAssistantHeaderProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);
  const [isTabsScrolling, setIsTabsScrolling] = useState(false);
  const scrollEndTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const tabs = useMemo(() => {
    // Ensure at least one tab exists (the new-chat runtime)
    if (conversationTabs.length === 0) {
      return [
        {
          id: null,
          titleCandidate: null,
          messageCount: 0,
          createdAt: Date.now(),
          lastTouchedAt: Date.now(),
        } as ConversationTabSummary,
      ];
    }
    return conversationTabs;
  }, [conversationTabs]);

  const updateScrollState = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;
    const left = el.scrollLeft;
    const maxLeft = el.scrollWidth - el.clientWidth;
    const threshold = 1;
    setCanScrollLeft(left > threshold);
    setCanScrollRight(maxLeft - left > threshold);
  }, []);

  const onWheelTabsScroll = useCallback((e: React.WheelEvent<HTMLDivElement>) => {
    const el = scrollRef.current;
    if (!el) return;
    if (el.scrollWidth <= el.clientWidth) return;
    // Convert vertical wheel to horizontal scroll (Cursor-like)
    if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
      e.preventDefault();
      el.scrollBy({ left: e.deltaY, behavior: 'auto' });
    }
  }, []);

  const onTabsScroll = useCallback(() => {
    updateScrollState();
    setIsTabsScrolling(true);
    if (scrollEndTimerRef.current) {
      clearTimeout(scrollEndTimerRef.current);
    }
    scrollEndTimerRef.current = setTimeout(() => {
      setIsTabsScrolling(false);
    }, 700);
  }, [updateScrollState]);

  useEffect(() => {
    return () => {
      if (scrollEndTimerRef.current) clearTimeout(scrollEndTimerRef.current);
    };
  }, []);

  useEffect(() => {
    updateScrollState();
    const onResize = () => updateScrollState();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [tabs.length, updateScrollState]);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const active = el.querySelector<HTMLElement>('[data-ai-tab-active="true"]');
    active?.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
    // Recompute masks after potential scrollIntoView
    setTimeout(() => updateScrollState(), 0);
  }, [currentConversationId, updateScrollState]);

  return (
    <div className="workbench-header flex items-center justify-between gap-2 px-3 py-2 shrink-0">
      <div className="flex-1 min-w-0">
        <div className="relative min-w-0 overflow-hidden">
          {canScrollLeft && (
            <div className="pointer-events-none absolute left-0 top-0 bottom-0 z-10 w-6 bg-gradient-to-r from-[color:var(--bg-panel)] via-[color:var(--bg-panel)]/88 to-transparent" />
          )}
          {canScrollRight && (
            <div className="pointer-events-none absolute right-0 top-0 bottom-0 z-10 w-6 bg-gradient-to-l from-[color:var(--bg-panel)] via-[color:var(--bg-panel)]/88 to-transparent" />
          )}
          <div
            ref={scrollRef}
            className={cn(
              'tabs-scroll h-8 flex items-center gap-1.5',
              isTabsScrolling ? 'tabs-scroll--scrolling' : 'tabs-scroll--idle'
            )}
            onScroll={onTabsScroll}
            onWheel={onWheelTabsScroll}
          >
            {tabs.map((tab) => {
              const isActive = tab.id === currentConversationId;
              const { display, full } = formatTabTitle(tab);
              return (
                <div
                  key={tab.id == null ? '__new__' : String(tab.id)}
                  data-ai-tab-active={isActive ? 'true' : undefined}
                  role="tab"
                  tabIndex={0}
                  className={cn(
                    'workbench-chip flex h-8 max-w-[220px] shrink-0 items-center gap-1.5 rounded-xl px-3 text-xs leading-none select-none group',
                    isActive ? 'workbench-chip--active text-[color:var(--text-primary)]' : 'theme-text-secondary hover:theme-text-primary'
                  )}
                  title={full}
                  onClick={() => onSelectTab(tab.id)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      onSelectTab(tab.id);
                    }
                  }}
                >
                  <span className="truncate min-w-0">{display}</span>
                  <button
                    type="button"
                    className={cn(
                      'workbench-icon-button h-5 w-5 p-0 shrink-0 transition-colors',
                      'opacity-0 group-hover:opacity-100 hover:bg-red-500/20 hover:text-red-400'
                    )}
                    aria-label="Close tab"
                    title="Close"
                    onClick={(e) => {
                      e.stopPropagation();
                      onCloseTab(tab.id);
                    }}
                    onPointerDown={(e) => e.stopPropagation()}
                  >
                    <X className="w-3 h-3" />
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2 theme-text-secondary shrink-0">
        <button
          type="button"
          className="workbench-icon-button"
          onClick={onNewChat}
          aria-label="New chat"
          title="New chat"
        >
          <Plus className="w-4 h-4" />
        </button>
        {accessToken && (
          <div className="relative">
            <button
              type="button"
              className="workbench-icon-button"
              onClick={(e) => {
                e.stopPropagation();
                setIsHistoryOpen((prev) => !prev);
              }}
              aria-label={historyAriaLabel}
              title={historyAriaLabel}
            >
              <History className="w-4 h-4" />
            </button>
            {isHistoryOpen && (
              <ConversationHistoryPanel
                open={isHistoryOpen}
                onClose={() => setIsHistoryOpen(false)}
                onSelectConversation={onSelectConversation}
                onNewChat={onNewChat}
                currentConversationId={currentConversationId}
              />
            )}
          </div>
        )}
        <button
          type="button"
          className="workbench-icon-button"
          aria-label="Close panel"
          title="Close panel"
          onClick={onClosePanel}
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
