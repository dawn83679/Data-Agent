import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useTheme } from '../../../hooks/useTheme';
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
  /** Tool name for detecting SQL/DDL content. */
  toolName?: string;
}

/** Detect if response contains SQL/DDL content that should be syntax highlighted. */
function isSqlContent(responseData: string, toolName?: string): boolean {
  if (!responseData?.trim()) return false;
  
  const lowerName = toolName?.toLowerCase() ?? '';
  const lowerData = responseData.trim().toLowerCase();
  
  // Check tool name for SQL/DDL indicators
  if (lowerName.includes('ddl') || lowerName.includes('sql') || lowerName.includes('query')) {
    return true;
  }
  
  // Check response content for SQL keywords
  const sqlKeywords = ['create table', 'alter table', 'drop table', 'select ', 'insert ', 'update ', 'delete ', 'create index'];
  return sqlKeywords.some(keyword => lowerData.includes(keyword));
}

/** Expandable detail for a generic tool run: Parameters and Response sections. */
export function ToolRunDetail({
  formattedParameters,
  isParametersJson,
  responseData,
  toolName,
}: ToolRunDetailProps) {
  const { theme } = useTheme();
  const shouldHighlightSql = isSqlContent(responseData, toolName);
  const syntaxTheme = theme === 'dark' ? oneDark : oneLight;

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
              style={syntaxTheme}
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
        {shouldHighlightSql ? (
          <div className="rounded overflow-hidden bg-black/10 dark:bg-black/20 text-[11px] max-h-[220px] overflow-auto">
            <SyntaxHighlighter
              language="sql"
              style={syntaxTheme}
              showLineNumbers={true}
              customStyle={{
                margin: 0,
                padding: '0.5rem',
                fontSize: '11px',
                lineHeight: 1.5,
                background: 'transparent',
              }}
              codeTagProps={{ style: { fontFamily: 'inherit' } }}
              PreTag="div"
            >
              {responseData || TOOL_RUN_EMPTY_PLACEHOLDER}
            </SyntaxHighlighter>
          </div>
        ) : (
          <pre className="py-1.5 px-2 rounded bg-black/10 dark:bg-black/20 font-mono text-[11px] overflow-x-auto max-h-[220px] overflow-y-auto whitespace-pre-wrap break-words">
            {responseData || TOOL_RUN_EMPTY_PLACEHOLDER}
          </pre>
        )}
      </div>
    </div>
  );
}
