import { useMemo, useRef, useEffect, useState, useImperativeHandle, forwardRef, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { parseMentionSegments } from './mentionTypes';
import { AGENT_COLORS, type AgentType } from './agentTypes';
import type { UseMentionReturn } from '../../hooks/useMention';

interface ChatInputAreaProps {
    input: string;
    agent: AgentType;
    mention?: UseMentionReturn;
    onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
    onCompositionStateChange?: (isComposing: boolean) => void;
    onKeyDown: (e: React.KeyboardEvent) => void;
}

export interface ChatInputAreaRef {
    replaceAtMentionToken: (replacement: string) => void;
    setIgnoreBlur: (ignore: boolean) => void;
}

/** Textarea input area with @mention highlighting mirror layer */
export const ChatInputArea = forwardRef<ChatInputAreaRef, ChatInputAreaProps>(
    function ChatInputArea({ input, agent, mention, onChange, onCompositionStateChange, onKeyDown }, ref) {
        const { t } = useTranslation();
        
        // 优化: 只在有 @ 符号时才解析和显示镜像层
        const hasMention = input.includes('@');
        const inputSegments = useMemo(() => {
            // 没有 @ 符号时返回空数组，不显示镜像层
            if (!hasMention) {
                return [];
            }
            return parseMentionSegments(input);
        }, [input, hasMention]);
        
        const textareaRef = useRef<HTMLTextAreaElement>(null);
        const mirrorRef = useRef<HTMLDivElement>(null);
        const [isSelecting, setIsSelecting] = useState(false);
        const [isComposing, setIsComposing] = useState(false);
        const ignoreBlurRef = useRef(false);
        const caretRef = useRef(0); // Save last reliable cursor position
        const prevMentionOpenRef = useRef(false);
        const prevMentionLevelRef = useRef<string>('');

        // Sync caret position whenever it changes
        const syncCaret = useCallback(() => {
            const textarea = textareaRef.current;
            if (textarea) {
                caretRef.current = textarea.selectionStart;
            }
        }, []);

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

        useEffect(() => {
            return () => {
                onCompositionStateChange?.(false);
            };
        }, [onCompositionStateChange]);

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
            if (!textarea) return;

            const MIN_HEIGHT = 40;
            const MAX_LINES = 14;

            const resize = () => {
                const computedStyle = window.getComputedStyle(textarea);
                let lineHeight = parseFloat(computedStyle.lineHeight);

                if (isNaN(lineHeight)) {
                    const fontSize = parseFloat(computedStyle.fontSize);
                    lineHeight = fontSize * 1.2;
                }

                const paddingTop = parseFloat(computedStyle.paddingTop);
                const paddingBottom = parseFloat(computedStyle.paddingBottom);
                const MAX_HEIGHT = lineHeight * MAX_LINES + paddingTop + paddingBottom;

                textarea.style.height = 'auto';
                const scrollHeight = textarea.scrollHeight;
                const newHeight = Math.min(Math.max(scrollHeight, MIN_HEIGHT), MAX_HEIGHT);

                textarea.style.height = `${newHeight}px`;
                if (mirror) {
                    mirror.style.height = `${newHeight}px`;
                }
            };

            // 使用 requestAnimationFrame 优化 resize 性能
            let rafId: number;
            const debouncedResize = () => {
                if (rafId) cancelAnimationFrame(rafId);
                rafId = requestAnimationFrame(resize);
            };

            debouncedResize();

            const ro = new ResizeObserver(debouncedResize);
            ro.observe(textarea);

            return () => {
                ro.disconnect();
                if (rafId) cancelAnimationFrame(rafId);
            };
        }, [input]);

        // Sync scroll position between textarea and mirror
        const handleScroll = useCallback((e: React.UIEvent<HTMLTextAreaElement>) => {
            if (mirrorRef.current) {
                mirrorRef.current.scrollTop = e.currentTarget.scrollTop;
            }
        }, []);

        // Detect text selection to toggle mirror visibility
        const handleSelect = useCallback(() => {
            const textarea = textareaRef.current;
            if (!textarea) return;

            const hasSelection = textarea.selectionStart !== textarea.selectionEnd;
            setIsSelecting(hasSelection);
            syncCaret(); // Update caret position
        }, [syncCaret]);

        const handleMouseUp = useCallback(() => {
            handleSelect();
        }, [handleSelect]);

        const handleKeyUp = useCallback(() => {
            handleSelect();
        }, [handleSelect]);

        const handleBlur = useCallback(() => {
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
        }, []);

        const handleCompositionStart = useCallback(() => {
            setIsComposing(true);
            onCompositionStateChange?.(true);
        }, [onCompositionStateChange]);

        const handleCompositionEnd = useCallback(() => {
            setIsComposing(false);
            onCompositionStateChange?.(false);
        }, [onCompositionStateChange]);

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

                <div className="relative min-h-10">
                    {/* Mirror layer: 只在有 @mention 时显示 */}
                    {hasMention && (
                        <div
                            ref={mirrorRef}
                            className="chat-input-mirror absolute inset-0 overflow-y-auto text-sm px-0 py-0 pr-10 whitespace-pre-wrap theme-text-primary"
                            aria-hidden="true"
                            style={{
                                scrollbarWidth: 'none',
                                msOverflowStyle: 'none',
                                scrollbarGutter: 'stable',
                                wordWrap: 'break-word',
                                overflowWrap: 'break-word',
                                lineHeight: 'normal',
                                opacity: isSelecting || isComposing ? 0 : 1,
                                pointerEvents: 'none',
                                willChange: 'opacity',
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
                    )}
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
                        className={`relative z-10 w-full bg-transparent text-sm px-0 py-0 pr-10 focus:outline-none placeholder:text-[var(--text-secondary)]/80 overflow-y-auto whitespace-pre-wrap ${agent === 'Agent' ? 'caret-blue-400' : 'caret-blue-300'}`}
                        style={{
                            resize: 'none',
                            minHeight: '40px',
                            scrollbarGutter: 'stable',
                            wordWrap: 'break-word',
                            overflowWrap: 'break-word',
                            lineHeight: 'normal',
                            color: (isSelecting || isComposing || !hasMention) ? 'var(--text-primary)' : 'transparent',
                            willChange: 'contents',
                        }}
                    />
                </div>
            </>
        );
    });
