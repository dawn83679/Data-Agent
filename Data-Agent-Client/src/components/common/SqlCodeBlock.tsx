import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useTheme } from '../../hooks/useTheme';
import { cn } from '../../lib/utils';

export type SqlCodeBlockVariant = 'block' | 'compact' | 'inline';

export interface SqlCodeBlockProps {
  sql: string;
  variant?: SqlCodeBlockVariant;
  language?: string;
  wrapLongLines?: boolean;
  className?: string;
}

/**
 * Unified SQL / code syntax highlighting component.
 *
 * Variants:
 *  - **block**   — bordered wrapper, theme-aware bg, 11px font (markdown code blocks)
 *  - **compact** — transparent bg, 12px font, no outer border (card SQL previews)
 *  - **inline**  — span-level, inherits parent font (ResultsPanel output log)
 */
export function SqlCodeBlock({
  sql,
  variant = 'block',
  language = 'sql',
  wrapLongLines = false,
  className,
}: SqlCodeBlockProps) {
  const { theme } = useTheme();
  const syntaxTheme = theme === 'dark' ? oneDark : oneLight;
  const syntaxBg = theme === 'dark' ? '#282c34' : '#fafafa';

  if (variant === 'inline') {
    return (
      <SyntaxHighlighter
        language={language}
        style={syntaxTheme}
        PreTag="span"
        CodeTag="span"
        customStyle={{
          background: 'transparent',
          padding: 0,
          margin: 0,
          fontFamily: 'inherit',
          fontSize: 'inherit',
          lineHeight: 'inherit',
        }}
        codeTagProps={{
          style: {
            fontFamily: 'inherit',
            fontSize: 'inherit',
            lineHeight: 'inherit',
            color: 'inherit',
          },
        }}
      >
        {sql}
      </SyntaxHighlighter>
    );
  }

  if (variant === 'compact') {
    return (
      <div className={cn('p-0 overflow-x-auto text-[12px]', className)}>
        <SyntaxHighlighter
          language={language}
          style={syntaxTheme}
          customStyle={{
            margin: 0,
            padding: '0.75rem',
            background: 'transparent',
            fontSize: '12px',
            textShadow: 'none',
            fontFamily:
              'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace',
          }}
          wrapLongLines={wrapLongLines}
        >
          {sql}
        </SyntaxHighlighter>
      </div>
    );
  }

  // variant === 'block'
  return (
    <div
      className={cn(
        'my-2 rounded border theme-border overflow-hidden max-h-[200px] overflow-y-auto text-[11px]',
        className,
      )}
    >
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
        {sql}
      </SyntaxHighlighter>
    </div>
  );
}
