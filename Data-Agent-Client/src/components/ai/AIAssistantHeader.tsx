import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { History, Plus, Settings as SettingsIcon, X } from 'lucide-react';
import { AISettings } from './AISettings';
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
  isSettingsOpen: boolean;
  setIsSettingsOpen: React.Dispatch<React.SetStateAction<boolean>>;
  currentConversationId: number | null;
  conversationTabs: ConversationTabSummary[];
  onSelectTab: (id: number | null) => void;
  onCloseTab: (id: number | null) => void;
  onSelectConversation: (conversation: Conversation) => void;
  onNewChat: () => void;
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
  isSettingsOpen,
  setIsSettingsOpen,
  currentConversationId,
  conversationTabs,
  onSelectTab,
  onCloseTab,
  onSelectConversation,
  onNewChat,
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
    <div className="flex items-center gap-2 px-3 py-2 theme-bg-panel border-b theme-border shrink-0">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5">
          <div className="relative flex-1 min-w-0 overflow-hidden">
            {canScrollLeft && (
              <div className="pointer-events-none absolute left-0 top-0 bottom-0 w-6 bg-gradient-to-r from-[var(--bg-panel)] to-transparent z-10" />
            )}
            {canScrollRight && (
              <div className="pointer-events-none absolute right-0 top-0 bottom-0 w-6 bg-gradient-to-l from-[var(--bg-panel)] to-transparent z-10" />
            )}

            <div
              ref={scrollRef}
              className={cn(
                'tabs-scroll h-7 flex items-center gap-1',
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
                      'h-7 px-2.5 rounded-md border text-[11px] leading-none shrink-0',
                      'transition-colors select-none max-w-[260px]',
                      'flex items-center gap-1.5 group',
                      isActive
                        ? 'theme-bg-main theme-border theme-text-primary'
                        : 'border-transparent theme-text-secondary hover:bg-[var(--bg-main)]/40 hover:theme-text-primary'
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
                        'p-0.5 rounded transition-colors shrink-0',
                        'opacity-0 group-hover:opacity-100',
                        'hover:bg-red-500/20 hover:text-red-400'
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

          <button
            type="button"
            className={cn(
              'h-7 w-7 rounded-md flex items-center justify-center shrink-0 transition-colors',
              'bg-[var(--bg-main)]/30 hover:bg-[var(--bg-main)]/50 theme-text-secondary hover:theme-text-primary'
            )}
            onClick={onNewChat}
            aria-label="New chat"
            title="New chat"
          >
            <Plus className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="flex items-center space-x-2 shrink-0">
        {accessToken && (
          <div className="relative">
            <History
              className="w-3.5 h-3.5 theme-text-secondary hover:theme-text-primary cursor-pointer transition-colors"
              onClick={(e) => {
                e.stopPropagation();
                setIsHistoryOpen((prev) => !prev);
              }}
              aria-label={historyAriaLabel}
            />
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
        <div className="relative">
          <SettingsIcon
            className="w-3.5 h-3.5 theme-text-secondary hover:theme-text-primary cursor-pointer transition-colors"
            onClick={() => setIsSettingsOpen((prev) => !prev)}
          />
          {isSettingsOpen && (
            <AISettings onClose={() => setIsSettingsOpen(false)} />
          )}
        </div>
        <X className="w-3.5 h-3.5 theme-text-secondary hover:theme-text-primary cursor-pointer transition-colors" />
      </div>
    </div>
  );
}
