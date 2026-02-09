import React from 'react';
import ReactMarkdown from 'react-markdown';
import { useTranslation } from 'react-i18next';
import { Bot, User } from 'lucide-react';
import { splitByMention, MENTION_PART_REGEX } from './mentionTypes';
import type { ChatResponseBlock } from '../../types/chat';
import { MessageBlockType } from '../../types/chat';
import type { ToolCallData, ToolResultData } from '../../types/chat';
import {
  TextBlock,
  ThoughtBlock,
  ToolRunBlock,
  markdownComponents,
} from './blocks';

const MENTION_COLOR_CLASS = 'text-cyan-400 font-medium';

function parseContentWithMentions(content: string): React.ReactNode[] {
  const parts = splitByMention(content);
  return parts.map((part, i) =>
    MENTION_PART_REGEX.test(part) ? (
      <span key={i} className={MENTION_COLOR_CLASS}>
        {part}
      </span>
    ) : (
      <React.Fragment key={i}>{part}</React.Fragment>
    )
  );
}

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  blocks?: ChatResponseBlock[];
}

/** Converts blocks to segments: merge consecutive TEXT; pair TOOL_CALL+TOOL_RESULT as TOOL_RUN. */
type Segment =
  | { kind: 'TEXT'; data: string }
  | { kind: 'THOUGHT'; data: string }
  | { kind: 'TOOL_RUN'; toolName: string; parametersData: string; responseData: string };

function parseToolCall(block: ChatResponseBlock): ToolCallData | null {
  if (!block.data) return null;
  try {
    const parsed = JSON.parse(block.data) as ToolCallData;
    return parsed?.toolName != null ? parsed : null;
  } catch {
    return null;
  }
}

function parseToolResult(block: ChatResponseBlock): ToolResultData | null {
  if (!block.data) return null;
  try {
    const parsed = JSON.parse(block.data) as ToolResultData;
    return parsed?.toolName != null ? parsed : null;
  } catch {
    return null;
  }
}

/**
 * Merges consecutive assistant messages so TOOL_CALL and TOOL_RESULT live in one bubble.
 * History API returns them as separate messages; merging here matches streaming behaviour (pair in same message).
 */
function mergeAssistantToolPairs(messages: Message[]): Message[] {
  const result: Message[] = [];
  for (let i = 0; i < messages.length; i++) {
    const msg = messages[i];
    if (msg.role !== 'assistant' || !msg.blocks?.length) {
      result.push(msg);
      continue;
    }
    const hasUnpairedToolCall = msg.blocks.some(
      (b, j) =>
        b.type === MessageBlockType.TOOL_CALL &&
        (msg.blocks![j + 1]?.type !== MessageBlockType.TOOL_RESULT)
    );
    const next = messages[i + 1];
    if (
      hasUnpairedToolCall &&
      next?.role === 'assistant' &&
      next.blocks?.length &&
      next.blocks.every((b) => b.type === MessageBlockType.TOOL_RESULT)
    ) {
      result.push({
        ...msg,
        blocks: [...(msg.blocks ?? []), ...next.blocks],
        content: msg.content || next.content,
      });
      i++;
    } else {
      result.push(msg);
    }
  }
  return result;
}

function blocksToSegments(blocks: ChatResponseBlock[]): Segment[] {
  const segments: Segment[] = [];
  let textBuffer = '';

  const flushText = () => {
    if (textBuffer) {
      segments.push({ kind: 'TEXT', data: textBuffer });
      textBuffer = '';
    }
  };

  for (let i = 0; i < blocks.length; i++) {
    const block = blocks[i];
    switch (block.type) {
      case MessageBlockType.TEXT:
        textBuffer += block.data ?? '';
        break;
      case MessageBlockType.THOUGHT:
        flushText();
        segments.push({ kind: 'THOUGHT', data: block.data ?? '' });
        break;
      case MessageBlockType.TOOL_CALL: {
        flushText();
        const callPayload = parseToolCall(block);
        const next = blocks[i + 1];
        const resultPayload = next?.type === MessageBlockType.TOOL_RESULT ? parseToolResult(next) : null;
        const sameTool = callPayload && resultPayload && callPayload.toolName === resultPayload.toolName;
        if (sameTool) {
          segments.push({
            kind: 'TOOL_RUN',
            toolName: callPayload.toolName,
            parametersData: callPayload.arguments ?? '',
            responseData: resultPayload.result ?? '',
          });
          i++;
        } else {
          segments.push({
            kind: 'TOOL_RUN',
            toolName: callPayload?.toolName ?? '',
            parametersData: callPayload?.arguments ?? '',
            responseData: '',
          });
        }
        break;
      }
      case MessageBlockType.TOOL_RESULT: {
        flushText();
        const resultPayload = parseToolResult(block);
        segments.push({
          kind: 'TOOL_RUN',
          toolName: resultPayload?.toolName ?? '',
          parametersData: '',
          responseData: resultPayload?.result ?? '',
        });
        break;
      }
      default:
        if (block.data) textBuffer += block.data;
        break;
    }
  }
  flushText();
  return segments;
}

function renderSegment(segment: Segment, index: number): React.ReactNode {
  const key = `seg-${index}-${segment.kind}`;
  switch (segment.kind) {
    case 'TEXT':
      return <TextBlock key={key} data={segment.data} />;
    case 'THOUGHT':
      return <ThoughtBlock key={key} data={segment.data} />;
    case 'TOOL_RUN':
      return (
        <ToolRunBlock
          key={key}
          toolName={segment.toolName}
          parametersData={segment.parametersData}
          responseData={segment.responseData}
        />
      );
  }
}

interface MessageListProps {
  messages: Message[];
  messagesEndRef: React.RefObject<HTMLDivElement>;
}

export function MessageList({ messages, messagesEndRef }: MessageListProps) {
  const { t } = useTranslation();
  const displayMessages = mergeAssistantToolPairs(messages);
  return (
    <div className="flex-1 overflow-y-auto p-3 space-y-4 no-scrollbar theme-bg-main">
      {displayMessages.map((msg) => (
        <div key={msg.id} className="flex flex-col w-full">
          <div className="flex items-center space-x-2 mb-1.5 opacity-60">
            {msg.role === 'assistant' && <Bot className="w-3 h-3 shrink-0" />}
            <span className="text-[10px] font-medium theme-text-secondary">
              {msg.role === 'assistant' ? t('ai.bot_name') : t('ai.you')}
            </span>
            {msg.role === 'user' && <User className="w-3 h-3 shrink-0" />}
          </div>
          {msg.role === 'user' ? (
            <div className="px-3 py-2 rounded-lg text-xs bg-primary/90 text-primary-foreground w-fit max-w-full">
              <p className="mb-0 leading-relaxed whitespace-pre-wrap">
                {parseContentWithMentions(msg.content)}
              </p>
            </div>
          ) : (
            <div className="text-xs theme-text-primary">
              {msg.blocks && msg.blocks.length > 0 ? (
                <div className="space-y-0">
                  {blocksToSegments(msg.blocks).map((seg, i) => renderSegment(seg, i))}
                </div>
              ) : (
                <ReactMarkdown components={markdownComponents}>{msg.content || ''}</ReactMarkdown>
              )}
            </div>
          )}
        </div>
      ))}
      <div ref={messagesEndRef} />
    </div>
  );
}
