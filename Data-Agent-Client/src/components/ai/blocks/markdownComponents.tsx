import React from 'react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { cn } from '../../../lib/utils';

export const markdownComponents: React.ComponentProps<typeof ReactMarkdown>['components'] = {
  code({ className, children, ...props }) {
    const match = /language-(\w+)/.exec(className || '');
    const language = match ? match[1] : '';
    const isInline = !match;
    if (!isInline) {
      const code = String(children).replace(/\n$/, '');
      return (
        <div className="my-2 rounded border theme-border overflow-hidden max-h-[200px] overflow-y-auto text-[11px]">
          <SyntaxHighlighter
            language={language || 'text'}
            style={oneDark}
            showLineNumbers={false}
            customStyle={{
              margin: 0,
              padding: '0.75rem 1rem',
              fontSize: '11px',
              lineHeight: 1.5,
              background: 'var(--code-bg, #282c34)',
            }}
            codeTagProps={{ style: { fontFamily: 'inherit' } }}
            PreTag="div"
          >
            {code}
          </SyntaxHighlighter>
        </div>
      );
    }
    return (
      <code className={cn('bg-accent/50 px-1 rounded text-[11px] font-mono', className)} {...props}>
        {children}
      </code>
    );
  },
  p: ({ children }: { children?: React.ReactNode }) => (
    <p className="mb-2 last:mb-0 leading-relaxed">{children}</p>
  ),
  ul: ({ children }: { children?: React.ReactNode }) => (
    <ul className="list-disc pl-4 mb-2">{children}</ul>
  ),
  ol: ({ children }: { children?: React.ReactNode }) => (
    <ol className="list-decimal pl-4 mb-2">{children}</ol>
  ),
};
