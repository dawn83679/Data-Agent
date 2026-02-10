import { Brain, History, Settings as SettingsIcon, X } from 'lucide-react';
import { AISettings } from './AISettings';
import { ConversationHistoryPanel } from './ConversationHistoryPanel';

export interface AIAssistantHeaderProps {
  title: string;
  historyAriaLabel: string;
  accessToken: boolean;
  isHistoryOpen: boolean;
  setIsHistoryOpen: React.Dispatch<React.SetStateAction<boolean>>;
  isSettingsOpen: boolean;
  setIsSettingsOpen: React.Dispatch<React.SetStateAction<boolean>>;
  currentConversationId: number | null;
  onSelectConversation: (id: number) => void;
  onNewChat: () => void;
}

export function AIAssistantHeader({
  title,
  historyAriaLabel,
  accessToken,
  isHistoryOpen,
  setIsHistoryOpen,
  isSettingsOpen,
  setIsSettingsOpen,
  currentConversationId,
  onSelectConversation,
  onNewChat,
}: AIAssistantHeaderProps) {
  return (
    <div className="flex items-center justify-between px-3 py-2 theme-bg-panel border-b theme-border shrink-0">
      <div className="flex items-center space-x-2">
        <Brain className="w-4 h-4 text-purple-400" />
        <span className="theme-text-primary text-xs font-bold">{title}</span>
      </div>
      <div className="flex items-center space-x-2">
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
