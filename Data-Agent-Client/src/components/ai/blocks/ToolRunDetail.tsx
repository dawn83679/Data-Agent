import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import {
  TOOL_RUN_SECTION_PARAMETERS,
  TOOL_RUN_SECTION_RESPONSE,
  TOOL_RUN_EMPTY_PLACEHOLDER,
} from '../../../constants/chat';

export interface ToolRunDetailProps {
  /** Formatted parameters string (may be pretty-printed JSON). */
  formattedParameters: string;
  /** When true, render parameters in SyntaxHighlighter as JSON. */
  isParametersJson: boolean;
  /** Raw response data from tool. */
  responseData: string;
}

/** Expandable detail for a generic tool run: Parameters and Response sections. */
export function ToolRunDetail({
  formattedParameters,
  isParametersJson,
  responseData,
}: ToolRunDetailProps) {
  return (
    <div className="mt-1 space-y-2 theme-text-primary">
      <div>
        <div className="text-[10px] font-semibold uppercase tracking-wide opacity-90 mb-1 flex items-center gap-1">
          {TOOL_RUN_SECTION_PARAMETERS}
          <span className="opacity-50" aria-hidden>☰</span>
        </div>
        {isParametersJson ? (
          <div className="rounded overflow-hidden bg-black/10 dark:bg-black/20 text-[11px] max-h-[220px] overflow-auto">
            <SyntaxHighlighter
              language="json"
              style={oneDark}
              showLineNumbers={false}
              customStyle={{
                margin: 0,
                padding: '0.375rem 0.5rem',
                fontSize: '11px',
                lineHeight: 1.45,
                background: 'transparent',
              }}
              codeTagProps={{ style: { fontFamily: 'inherit' } }}
              PreTag="div"
            >
              {formattedParameters}
            </SyntaxHighlighter>
          </div>
        ) : (
          <pre className="py-1.5 px-2 rounded bg-black/10 dark:bg-black/20 font-mono text-[11px] overflow-x-auto whitespace-pre-wrap break-words">
            {formattedParameters || TOOL_RUN_EMPTY_PLACEHOLDER}
          </pre>
        )}
      </div>
      <div>
        <div className="text-[10px] font-semibold uppercase tracking-wide opacity-90 mb-1">
          {TOOL_RUN_SECTION_RESPONSE}
        </div>
        <pre className="py-1.5 px-2 rounded bg-black/10 dark:bg-black/20 font-mono text-[11px] overflow-x-auto max-h-[220px] overflow-y-auto whitespace-pre-wrap break-words">
          {responseData || TOOL_RUN_EMPTY_PLACEHOLDER}
        </pre>
      </div>
    </div>
  );
}
