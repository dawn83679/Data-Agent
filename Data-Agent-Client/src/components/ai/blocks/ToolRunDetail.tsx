import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useTheme } from '../../../hooks/useTheme';
import {
  TOOL_RUN_SECTION_PARAMETERS,
  TOOL_RUN_SECTION_RESPONSE,
  TOOL_RUN_EMPTY_PLACEHOLDER,
} from '../../../constants/chat';

export interface ToolRunDetailProps {
  formattedParameters: string;
  responseData: string;
}

/** Expandable detail for a generic tool run: Parameters and Response sections. */
export function ToolRunDetail({
  formattedParameters,
  responseData,
}: ToolRunDetailProps) {
  const { theme } = useTheme();
  const syntaxTheme = theme === 'dark' ? oneDark : oneLight;
  const sectionShellClassName = theme === 'dark'
    ? 'rounded-lg border border-white/8 bg-black/18'
    : 'rounded-lg border border-slate-200 bg-slate-50/90';

  const highlighterProps = {
    language: 'json',
    style: syntaxTheme,
    showLineNumbers: false,
    customStyle: {
      margin: 0,
      padding: '0.375rem 0.5rem',
      fontSize: '11px',
      lineHeight: 1.45,
      background: 'transparent',
    },
    codeTagProps: { style: { fontFamily: 'inherit' } },
    PreTag: 'div' as const,
  };

  return (
    <div className="mt-1 space-y-2 theme-text-primary">
      <div>
        <div className="text-[10px] font-semibold uppercase tracking-wide opacity-90 mb-1 flex items-center gap-1">
          {TOOL_RUN_SECTION_PARAMETERS}
          <span className="opacity-50" aria-hidden>☰</span>
        </div>
        <div className={`${sectionShellClassName} text-[11px] max-h-[220px] overflow-auto`}>
          <SyntaxHighlighter {...highlighterProps}>
            {formattedParameters || TOOL_RUN_EMPTY_PLACEHOLDER}
          </SyntaxHighlighter>
        </div>
      </div>
      <div>
        <div className="text-[10px] font-semibold uppercase tracking-wide opacity-90 mb-1">
          {TOOL_RUN_SECTION_RESPONSE}
        </div>
        <div className={`${sectionShellClassName} text-[11px] max-h-[220px] overflow-auto`}>
          <SyntaxHighlighter {...highlighterProps}>
            {responseData || TOOL_RUN_EMPTY_PLACEHOLDER}
          </SyntaxHighlighter>
        </div>
      </div>
    </div>
  );
}
