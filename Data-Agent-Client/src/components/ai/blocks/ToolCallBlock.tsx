import React from 'react';
import { Wrench } from 'lucide-react';
import type { ChatResponseBlock, ToolCallData } from '../../../types/chat';

export interface ToolCallBlockProps {
  block: ChatResponseBlock;
}

function parsePayload(block: ChatResponseBlock): ToolCallData | null {
  if (!block.data) return null;
  try {
    const parsed = JSON.parse(block.data) as ToolCallData;
    return parsed?.toolName != null ? parsed : null;
  } catch {
    return null;
  }
}

/** Renders a TOOL_CALL block (tool name and parameters). */
export function ToolCallBlock({ block }: ToolCallBlockProps) {
  const payload = parsePayload(block);
  if (!payload) {
    return <div className="mb-2 text-[11px] theme-text-secondary">{block.data ?? '—'}</div>;
  }
  return (
    <div className="mb-2 rounded border theme-border overflow-hidden text-[11px]">
      <div className="px-2 py-1.5 theme-bg-panel border-b theme-border flex items-center gap-1.5 font-medium">
        <Wrench className="w-3 h-3 text-amber-500" />
        <span>PARAMETERS</span>
      </div>
      <div className="px-2 py-1.5 space-y-1">
        <div>
          <span className="theme-text-secondary">toolName:</span>{' '}
          <code className="bg-accent/50 px-1 rounded">{payload.toolName}</code>
        </div>
        <div>
          <span className="theme-text-secondary">arguments:</span>
          <pre className="mt-0.5 p-1.5 rounded bg-black/20 overflow-x-auto text-[10px] whitespace-pre-wrap break-all">
            {payload.arguments || '—'}
          </pre>
        </div>
      </div>
    </div>
  );
}
