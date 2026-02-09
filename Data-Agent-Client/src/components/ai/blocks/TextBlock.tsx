import React from 'react';
import ReactMarkdown from 'react-markdown';
import { markdownComponents } from './markdownComponents';

export interface TextBlockProps {
  data: string;
}

/** Renders a TEXT block (Markdown body). */
export function TextBlock({ data }: TextBlockProps) {
  if (!data) return null;
  return (
    <div className="mb-2 last:mb-0">
      <ReactMarkdown components={markdownComponents}>{data}</ReactMarkdown>
    </div>
  );
}
