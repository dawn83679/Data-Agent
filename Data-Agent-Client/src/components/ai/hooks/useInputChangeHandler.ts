import { useCallback } from 'react';
import type { UseMentionReturn } from '../../../hooks/useMention';

interface UseInputChangeHandlerOptions {
  setInput: (value: string) => void;
  mention: UseMentionReturn;
  onSlashOpen: (open: boolean) => void;
  onSlashQuery: (query: string) => void;
  onSlashHighlight: (index: number) => void;
  slashStateRef: React.MutableRefObject<{ start: number; query: string }>;
}

/** Handles input change detection for @mentions and /slash commands */
export function useInputChangeHandler({
  setInput,
  mention,
  onSlashOpen,
  onSlashQuery,
  onSlashHighlight,
  slashStateRef,
}: UseInputChangeHandlerOptions) {
  return useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const value = e.target.value;
      setInput(value);
      const cursorPos = e.target.selectionStart ?? value.length;
      const beforeCursor = value.slice(0, cursorPos);

      // Handle @mention detection
      const lastAt = beforeCursor.lastIndexOf('@');
      const afterAt = lastAt !== -1 ? beforeCursor.slice(lastAt + 1) : '';
      const charBeforeAt = lastAt > 0 ? beforeCursor[lastAt - 1] : ' ';
      const atIsAtStartOrAfterSpace = lastAt === 0 || /\s/.test(charBeforeAt);
      const isValidMentionTrigger =
        lastAt !== -1 &&
        atIsAtStartOrAfterSpace &&
        (afterAt.length === 0 || !/[\s]/.test(afterAt));

      if (isValidMentionTrigger) {
        mention.openMention();
        onSlashOpen(false);
        return;
      }

      if (!value.includes('@') || (value.length > 0 && !value.trim().includes('@'))) {
        mention.closeMention();
      }

      // Handle /slash command detection
      const lastSlash = beforeCursor.lastIndexOf('/');
      const charBeforeSlash = lastSlash > 0 ? beforeCursor[lastSlash - 1] : ' ';
      const slashAtStartOrAfterSpace = lastSlash === 0 || /\s/.test(charBeforeSlash);
      const query = lastSlash !== -1 ? beforeCursor.slice(lastSlash + 1, cursorPos) : '';
      const queryHasSpace = /\s/.test(query);

      if (lastSlash !== -1 && slashAtStartOrAfterSpace && !queryHasSpace && cursorPos > lastSlash) {
        slashStateRef.current = { start: lastSlash, query };
        onSlashQuery(query);
        onSlashOpen(true);
        onSlashHighlight(0);
      } else {
        onSlashOpen(false);
      }
    },
    [setInput, mention.openMention, mention.closeMention, onSlashOpen, onSlashQuery, onSlashHighlight, slashStateRef]
  );
}
