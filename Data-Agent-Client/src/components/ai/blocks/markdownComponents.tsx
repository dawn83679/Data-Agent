import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useTheme } from '../../../hooks/useTheme';
import { cn } from '../../../lib/utils';

export const markdownRemarkPlugins = [remarkGfm];

export function useMarkdownComponents(): React.ComponentProps<typeof ReactMarkdown>['components'] {
  const { theme } = useTheme();
  const syntaxTheme = theme === 'dark' ? oneDark : oneLight;
  const syntaxBg = theme === 'dark' ? '#282c34' : '#fafafa';

  return {
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
              style={syntaxTheme}
              showLineNumbers={false}
              customStyle={{
                margin: 0,
                padding: '0.75rem 1rem',
                fontSize: '11px',
                lineHeight: 1.5,
                background: syntaxBg,
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
    table: ({ children }: { children?: React.ReactNode }) => (
      <div className="my-2 overflow-x-auto">
        <table className="w-full border-collapse border theme-border">{children}</table>
      </div>
    ),
    thead: ({ children }: { children?: React.ReactNode }) => (
      <thead className="theme-bg-hover">{children}</thead>
    ),
    tbody: ({ children }: { children?: React.ReactNode }) => <tbody>{children}</tbody>,
    tr: ({ children }: { children?: React.ReactNode }) => (
      <tr className="border-b theme-border">{children}</tr>
    ),
    th: ({ children }: { children?: React.ReactNode }) => (
      <th className="border theme-border px-2 py-1.5 text-left font-medium theme-text-secondary">
        {children}
      </th>
    ),
    td: ({ children }: { children?: React.ReactNode }) => (
      <td className="border theme-border px-2 py-1.5 text-left">{children}</td>
    ),
    img: ({ src, alt }: { src?: string; alt?: string }) => {
      // Filter out Aliyun OSS images (already shown in McpToolBlock)
      if (src && src.includes('alipayobjects.com')) {
        return null;
      }
      return <img src={src} alt={alt} className="max-w-full h-auto rounded my-2" />;
    },
  };
}
