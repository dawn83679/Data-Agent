import ReactMarkdown from 'react-markdown';
import { useMarkdownComponents, markdownRemarkPlugins } from './markdownComponents';

export interface TextBlockProps {
  data: string;
}

/** Renders a TEXT block (Markdown body). */
export function TextBlock({ data }: TextBlockProps) {
  const markdownComponents = useMarkdownComponents();
  if (!data) return null;
  return (
    <div className="mb-2 last:mb-0">
      <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>{data}</ReactMarkdown>
    </div>
  );
}
