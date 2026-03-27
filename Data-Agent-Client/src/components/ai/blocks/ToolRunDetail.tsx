import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useTheme } from '../../../hooks/useTheme';
import {
  TOOL_RUN_SECTION_PARAMETERS,
  TOOL_RUN_SECTION_RESPONSE,
  TOOL_RUN_EMPTY_PLACEHOLDER,
} from '../../../constants/chat';
import {
  getToolSectionShellClassName,
  TOOL_SECTION_TITLE_CLASSNAME,
} from './toolRunStyles';

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
  const sectionShellClassName = getToolSectionShellClassName(theme);

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
    <div className="space-y-3 theme-text-primary">
      <div>
        <div className={TOOL_SECTION_TITLE_CLASSNAME}>
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
        <div className={TOOL_SECTION_TITLE_CLASSNAME}>
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
