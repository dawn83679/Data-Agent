import React from 'react';
import ReactMarkdown from 'react-markdown';
import { markdownComponents } from './markdownComponents';

export interface ThoughtBlockProps {
  data: string;
}

/** Renders a THOUGHT block (reasoning content). */
export function ThoughtBlock({ data }: ThoughtBlockProps) {
  if (!data) return null;
  return (
    <div className="mb-2 last:mb-0 opacity-80 border-l-2 border-purple-500/50 pl-2 text-[11px] theme-text-secondary">
      <ReactMarkdown components={markdownComponents}>{data}</ReactMarkdown>
    </div>
  );
}
