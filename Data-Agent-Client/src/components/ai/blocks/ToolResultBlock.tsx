import React from 'react';
import { CheckCircle } from 'lucide-react';
import type { ChatResponseBlock, ToolResultData } from '../../../types/chat';

export interface ToolResultBlockProps {
  block: ChatResponseBlock;
}

function parsePayload(block: ChatResponseBlock): ToolResultData | null {
  if (!block.data) return null;
  try {
    const parsed = JSON.parse(block.data) as ToolResultData;
    return parsed?.toolName != null ? parsed : null;
  } catch {
    return null;
  }
}

/** Renders a TOOL_RESULT block (tool response). */
export function ToolResultBlock({ block }: ToolResultBlockProps) {
  const payload = parsePayload(block);
  if (!payload) {
    return <div className="mb-2 text-[11px] theme-text-secondary">{block.data ?? '—'}</div>;
  }
  return (
    <div className="mb-2 rounded border theme-border overflow-hidden text-[11px]">
      <div className="px-2 py-1.5 theme-bg-panel border-b theme-border flex items-center gap-1.5 font-medium">
        <CheckCircle className="w-3 h-3 text-green-500" />
        <span>RESPONSE</span>
      </div>
      <div className="px-2 py-1.5">
        <div className="mb-1">
          <span className="theme-text-secondary">toolName:</span>{' '}
          <code className="bg-accent/50 px-1 rounded">{payload.toolName}</code>
        </div>
        <pre className="mt-0.5 p-1.5 rounded bg-black/20 overflow-x-auto max-h-[200px] overflow-y-auto text-[10px] whitespace-pre-wrap break-all">
          {payload.result ?? '—'}
        </pre>
      </div>
    </div>
  );
}
