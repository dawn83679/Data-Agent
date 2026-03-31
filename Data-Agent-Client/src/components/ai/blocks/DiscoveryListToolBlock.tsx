import { useMemo, useState } from 'react';
import { CheckCircle, ChevronDown, ChevronRight, Database, XCircle } from 'lucide-react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import {
  TOOL_RUN_EMPTY_PLACEHOLDER,
  TOOL_RUN_LABEL_FAILED,
  TOOL_RUN_LABEL_RAN,
  TOOL_RUN_SECTION_PARAMETERS,
  TOOL_RUN_SECTION_RESPONSE,
} from '../../../constants/chat';
import { useTheme } from '../../../hooks/useTheme';
import { cn } from '../../../lib/utils';
import {
  getDiscoveryItemLabel,
  getToolDisplayName,
  parseSqlDiscoveryListResult,
} from './sqlDiscoveryToolUtils';
import {
  getToolCardClassName,
  getToolSectionShellClassName,
  TOOL_CARD_CONTENT_CLASSNAME,
  TOOL_CARD_HEADER_CLASSNAME,
  TOOL_CARD_META_CLASSNAME,
  TOOL_SECTION_TITLE_CLASSNAME,
} from './toolRunStyles';

export interface DiscoveryListToolBlockProps {
  toolName: string;
  formattedParameters: string;
  responseData: string;
  responseError: boolean;
}

function pluralize(count: number, noun: string): string {
  return `${count} ${noun}${count === 1 ? '' : 's'}`;
}

export function DiscoveryListToolBlock({
  toolName,
  formattedParameters,
  responseData,
  responseError,
}: DiscoveryListToolBlockProps) {
  const { theme } = useTheme();
  const parsed = useMemo(() => parseSqlDiscoveryListResult(responseData), [responseData]);
  const [collapsed, setCollapsed] = useState(true);
  const itemLabel = getDiscoveryItemLabel(toolName);
  const displayName = getToolDisplayName(toolName);
  const syntaxTheme = theme === 'dark' ? oneDark : oneLight;
  const sectionShellClassName = getToolSectionShellClassName(theme);
  const isHeaderError = responseError || !parsed.success;
  const metaText = parsed.items.length > 0
    ? pluralize(parsed.items.length, itemLabel)
    : isHeaderError
      ? 'Error'
      : 'Empty';

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
    <div className={getToolCardClassName(!collapsed)}>
      <button
        type="button"
        onClick={() => setCollapsed((current) => !current)}
        className={TOOL_CARD_HEADER_CLASSNAME}
      >
        {isHeaderError ? (
          <XCircle className="w-3.5 h-3.5 text-red-500 shrink-0" aria-label="Failed" />
        ) : (
          <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" aria-hidden />
        )}
        <span className="min-w-0 flex-1 truncate text-[12px] font-medium theme-text-primary">
          {isHeaderError ? TOOL_RUN_LABEL_FAILED : TOOL_RUN_LABEL_RAN}
          {displayName}
        </span>
        <span className={cn(TOOL_CARD_META_CLASSNAME, 'shrink-0')}>
          {metaText}
        </span>
        <span className="shrink-0 theme-text-secondary">
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <div className={cn(TOOL_CARD_CONTENT_CLASSNAME, 'space-y-3 theme-text-primary')}>
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
            <div className={cn(sectionShellClassName, 'space-y-3 px-3 py-3 text-[11px]')}>
              {parsed.message && (
                <div className="theme-text-secondary leading-5">
                  {parsed.message}
                </div>
              )}

              {parsed.items.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {parsed.items.map((item) => (
                    <span
                      key={item}
                      className="inline-flex items-center gap-1 rounded-full border theme-border px-2 py-1 text-[10px] theme-text-primary"
                    >
                      <Database className="h-3 w-3 theme-text-secondary" aria-hidden />
                      {item}
                    </span>
                  ))}
                </div>
              ) : (
                <div className="rounded border theme-border px-3 py-3 theme-text-secondary">
                  {parsed.message || `No ${itemLabel}s returned.`}
                </div>
              )}

              {parsed.prettyJson && (
                <details className="group">
                  <summary className="cursor-pointer list-none text-[10px] font-medium uppercase tracking-[0.08em] theme-text-secondary">
                    Raw JSON
                  </summary>
                  <div className="mt-2 max-h-[220px] overflow-auto">
                    <SyntaxHighlighter {...highlighterProps}>
                      {parsed.prettyJson || TOOL_RUN_EMPTY_PLACEHOLDER}
                    </SyntaxHighlighter>
                  </div>
                </details>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
