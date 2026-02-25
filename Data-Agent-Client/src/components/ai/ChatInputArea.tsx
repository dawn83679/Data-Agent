import { useMemo, useRef, useEffect, useState, useImperativeHandle, forwardRef } from 'react';
import { useTranslation } from 'react-i18next';
import { parseMentionSegments } from './mentionTypes';
import { AGENT_COLORS, type AgentType } from './agentTypes';
import type { UseMentionReturn } from '../../hooks/useMention';
import { I18N_KEYS } from '../../constants/i18nKeys';

interface ChatInputAreaProps {
    input: string;
    agent: AgentType;
    mention?: UseMentionReturn;
    onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
    onKeyDown: (e: React.KeyboardEvent) => void;
}

export interface ChatInputAreaRef {
    replaceAtMentionToken: (replacement: string) => void;
    setIgnoreBlur: (ignore: boolean) => void;
}

/** Textarea input area with @mention highlighting mirror layer */
export const ChatInputArea = forwardRef<ChatInputAreaRef, ChatInputAreaProps>(
    function ChatInputArea({ input, agent, mention, onChange, onKeyDown }, ref) {
        const { t } = useTranslation();
        const inputSegments = useMemo(() => parseMentionSegments(input), [input]);
        const textareaRef = useRef<HTMLTextAreaElement>(null);
        const mirrorRef = useRef<HTMLDivElement>(null);
        const [isSelecting, setIsSelecting] = useState(false);
        const [isComposing, setIsComposing] = useState(false);
        const ignoreBlurRef = useRef(false);
        const caretRef = useRef(0); // Save last reliable cursor position
        const prevMentionOpenRef = useRef(false);
        const prevMentionLevelRef = useRef<string>('');

        // Sync caret position whenever it changes
        const syncCaret = () => {
            const textarea = textareaRef.current;
            if (textarea) {
                caretRef.current = textarea.selectionStart;
            }
        };

        // Monitor mention popup state changes to detect table selection
        useEffect(() => {
            if (!mention) return;

            const wasOpen = prevMentionOpenRef.current;
            const isOpen = mention.mentionOpen;
            const wasTable = prevMentionLevelRef.current === 'table';

            // Detect when mention popup closes from table level (user clicked a table)
            if (wasOpen && !isOpen && wasTable) {
                // Popup just closed from table level, focus back to textarea
                setTimeout(() => {
                    textareaRef.current?.focus();
                }, 0);
            }

            prevMentionOpenRef.current = isOpen;
            prevMentionLevelRef.current = mention.mentionLevel;
        }, [mention?.mentionOpen, mention?.mentionLevel]);

        // Expose methods to parent via ref
        useImperativeHandle(ref, () => ({
            replaceAtMentionToken: (replacement: string) => {
                const textarea = textareaRef.current;
                if (!textarea) return;

                const text = textarea.value;
                // Use saved caret position instead of current selectionStart (which may be 0 after blur)
                const cursorPos = caretRef.current;

                // Find the last @ before cursor
                const beforeCursor = text.slice(0, cursorPos);
                const lastAtIndex = beforeCursor.lastIndexOf('@');

                if (lastAtIndex === -1) return;

                // Replace from @ to cursor with the replacement
                const newText = text.slice(0, lastAtIndex) + replacement + text.slice(cursorPos);
                const newCursorPos = lastAtIndex + replacement.length;

                // Use native setter to update value
                const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                    window.HTMLTextAreaElement.prototype,
                    'value'
                )?.set;

                if (nativeInputValueSetter) {
                    nativeInputValueSetter.call(textarea, newText);

                    // Dispatch input event to trigger React onChange
                    const event = new Event('input', { bubbles: true });
                    textarea.dispatchEvent(event);

                    // Restore cursor position and focus
                    textarea.focus();
                    textarea.setSelectionRange(newCursorPos, newCursorPos);
                    caretRef.current = newCursorPos;
                }
            },
            setIgnoreBlur: (ignore: boolean) => {
                ignoreBlurRef.current = ignore;
            },
        }), []);

        // Auto-resize textarea based on content (min 96px, max ~14 lines)
        useEffect(() => {
            const textarea = textareaRef.current;
            const mirror = mirrorRef.current;
            if (!textarea || !mirror) return;

            const MIN_HEIGHT = 96;
            const MAX_LINES = 14; // 12-16 lines for desktop, similar to GPT

            const resize = () => {
                // Calculate line height
                const computedStyle = window.getComputedStyle(textarea);
                let lineHeight = parseFloat(computedStyle.lineHeight);

                // If lineHeight is 'normal' or NaN, calculate from fontSize
                if (isNaN(lineHeight)) {
                    const fontSize = parseFloat(computedStyle.fontSize);
                    lineHeight = fontSize * 1.2; // Default line-height multiplier
                }

                const paddingTop = parseFloat(computedStyle.paddingTop);
                const paddingBottom = parseFloat(computedStyle.paddingBottom);

                // Calculate max height based on lines
                const MAX_HEIGHT = lineHeight * MAX_LINES + paddingTop + paddingBottom;

                // Reset to auto to get real scrollHeight
                textarea.style.height = 'auto';
                mirror.style.height = 'auto';

                // Calculate new height: clamp(scrollHeight, MIN_HEIGHT, MAX_HEIGHT)
                const scrollHeight = textarea.scrollHeight;
                const newHeight = Math.min(Math.max(scrollHeight, MIN_HEIGHT), MAX_HEIGHT);

                textarea.style.height = `${newHeight}px`;
                mirror.style.height = `${newHeight}px`;
            };

            resize();

            // Watch for layout changes
            const ro = new ResizeObserver(resize);
            ro.observe(textarea);

            return () => ro.disconnect();
        }, [input]);

        // Sync scroll position between textarea and mirror
        const handleScroll = (e: React.UIEvent<HTMLTextAreaElement>) => {
            if (mirrorRef.current) {
                mirrorRef.current.scrollTop = e.currentTarget.scrollTop;
            }
        };

        // Detect text selection to toggle mirror visibility
        const handleSelect = () => {
            const textarea = textareaRef.current;
            if (!textarea) return;

            const hasSelection = textarea.selectionStart !== textarea.selectionEnd;
            setIsSelecting(hasSelection);
            syncCaret(); // Update caret position
        };

        const handleMouseUp = () => {
            handleSelect();
        };

        const handleKeyUp = () => {
            handleSelect();
        };

        const handleBlur = () => {
            // Don't close popups if ignoreBlur is set (e.g., when clicking mention items)
            if (ignoreBlurRef.current) {
                ignoreBlurRef.current = false;
                // Focus back immediately to prevent selection loss
                setTimeout(() => {
                    textareaRef.current?.focus();
                }, 0);
                return;
            }
            // Normal blur handling can go here if needed
        };

        const handleCompositionStart = () => {
            setIsComposing(true);
        };

        const handleCompositionEnd = () => {
            setIsComposing(false);
        };

        return (
            <>
                <style>{`
        .chat-input-mirror,
        .chat-input-mirror * {
          user-select: none !important;
          -webkit-user-select: none !important;
          -moz-user-select: none !important;
          -ms-user-select: none !important;
          pointer-events: none !important;
        }
        
        textarea[data-ai-input]::selection {
          background-color: rgba(59, 130, 246, 0.2) !important;
        }
        
        textarea[data-ai-input]::-moz-selection {
          background-color: rgba(59, 130, 246, 0.2) !important;
        }
      `}</style>

                <div className="relative min-h-24">
                    {/* Mirror layer: same layout as textarea, shows colored @mention text */}
                    <div
                        ref={mirrorRef}
                        className="chat-input-mirror absolute inset-0 overflow-y-auto text-xs p-3 whitespace-pre-wrap theme-text-primary"
                        aria-hidden="true"
                        style={{
                            scrollbarWidth: 'none',
                            msOverflowStyle: 'none',
                            scrollbarGutter: 'stable',
                            wordWrap: 'break-word',
                            overflowWrap: 'break-word',
                            lineHeight: 'normal',
                            opacity: isSelecting || isComposing ? 0 : 1,
                            pointerEvents: 'none'
                        }}
                    >
                        {inputSegments.map((seg, i) =>
                                seg.type === 'mention' ? (
                                    <span key={i} className={AGENT_COLORS[agent].mentionText}>
              {seg.text}
            </span>
                                ) : (
                                    <span key={i}>{seg.text}</span>
                                )
                        )}
                    </div>
                    <textarea
                        ref={textareaRef}
                        id="chat-input-textarea"
                        name="chat-input"
                        data-ai-input
                        value={input}
                        onChange={onChange}
                        onKeyDown={onKeyDown}
                        onKeyUp={handleKeyUp}
                        onMouseUp={handleMouseUp}
                        onBlur={handleBlur}
                        onScroll={handleScroll}
                        onCompositionStart={handleCompositionStart}
                        onCompositionEnd={handleCompositionEnd}
                        placeholder={t('ai.placeholder_mention')}
                        className={`relative z-10 w-full bg-transparent text-xs p-3 focus:outline-none placeholder:text-muted-foreground/50 overflow-y-auto whitespace-pre-wrap ${agent === 'Agent' ? 'caret-violet-400' : 'caret-amber-400'}`}
                        style={{
                            resize: 'none',
                            minHeight: '96px',
                            scrollbarGutter: 'stable',
                            wordWrap: 'break-word',
                            overflowWrap: 'break-word',
                            lineHeight: 'normal',
                            color: isSelecting || isComposing ? 'var(--text-primary)' : 'transparent'
                        }}
                    />
                </div>
            </>
        );
    });
          ) : (
            <span key={i}>{seg.text}</span>
          )
        )}
      </div>
      <textarea
        data-ai-input
        value={input}
        onChange={onChange}
        onKeyDown={onKeyDown}
        placeholder={t(I18N_KEYS.AI.PLACEHOLDER_MENTION)}
        className={`relative z-10 w-full h-24 bg-transparent text-xs p-3 focus:outline-none resize-none placeholder:text-muted-foreground/50 text-transparent min-h-0 ${agent === 'Agent' ? 'caret-violet-400' : 'caret-amber-400'}`}
      />
    </div>
  );
}
