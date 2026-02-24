import { useCallback, useRef } from 'react';
import { useMention } from '../../../hooks/useMention';
import { splitByMention, MENTION_PART_REGEX } from '../mentionTypes';
import type { ChatContext } from '../../../types/chat';

interface UseMentionLogicReturn {
  mention: ReturnType<typeof useMention>;
  inputRef: React.MutableRefObject<string>;
  parseExistingShortNames: (text: string) => Set<string>;
}

interface UseMentionLogicOptions {
  input: string;
  setChatContext: React.Dispatch<React.SetStateAction<ChatContext>>;
  setInput: (value: string) => void;
}

/** Manages mention state and logic with duplicate detection */
export function useMentionLogic({
  input,
  setChatContext,
  setInput,
}: UseMentionLogicOptions): UseMentionLogicReturn {
  const inputRef = useRef(input);
  inputRef.current = input;

  /** Parse last-segment names from existing @mentions for duplicate detection. */
  const parseExistingShortNames = useCallback((text: string): Set<string> => {
    const parts = splitByMention(text).filter((p) => MENTION_PART_REGEX.test(p));
    return new Set(
      parts.map((m) => {
        const path = m.slice(1);
        return path.includes('/') ? (path.split('/').pop() ?? path) : path;
      })
    );
  }, []);

  const mention = useMention({
    setChatContext,
    onConfirmDisplay: ({ short: shortName, full: fullPath }) => {
      const prev = inputRef.current;
      const idx = prev.lastIndexOf('@');
      const beforeCurrentAt = idx >= 0 ? prev.slice(0, idx) : '';
      const existingShortNames = parseExistingShortNames(beforeCurrentAt);
      const hasDuplicate = existingShortNames.has(shortName);
      const displayText = hasDuplicate ? fullPath : `@${shortName}`;

      if (idx === -1) {
        setInput(prev + (prev && !prev.endsWith(' ') ? ' ' : '') + displayText);
      } else {
        setInput(prev.slice(0, idx) + displayText);
      }
    },
  });

  return {
    mention,
    inputRef,
    parseExistingShortNames,
  };
}
