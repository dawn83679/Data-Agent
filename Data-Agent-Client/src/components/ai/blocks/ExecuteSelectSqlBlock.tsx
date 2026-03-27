import { useEffect, useMemo, useState } from 'react';
import { CheckCircle, ChevronDown, ChevronRight, XCircle } from 'lucide-react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useTranslation } from 'react-i18next';
import {
  TOOL_RUN_EMPTY_PLACEHOLDER,
  TOOL_RUN_LABEL_FAILED,
  TOOL_RUN_LABEL_RAN,
  TOOL_RUN_SECTION_PARAMETERS,
  TOOL_RUN_SECTION_RESPONSE,
} from '../../../constants/chat';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { useTheme } from '../../../hooks/useTheme';
import { cn } from '../../../lib/utils';
import { LightDataTable } from './LightDataTable';
import { parseExecuteSelectResponse } from './executeSelectTypes';
import {
  getToolCardClassName,
  getToolSectionShellClassName,
  TOOL_CARD_CONTENT_CLASSNAME,
  TOOL_CARD_HEADER_CLASSNAME,
  TOOL_CARD_META_CLASSNAME,
  TOOL_SECTION_TITLE_CLASSNAME,
} from './toolRunStyles';

export interface ExecuteSelectSqlBlockProps {
  toolName: string;
  formattedParameters: string;
  responseData: string;
  responseError: boolean;
}

const PREVIEW_ROW_LIMIT = 10;

export function ExecuteSelectSqlBlock({
  toolName,
  formattedParameters,
  responseData,
  responseError,
}: ExecuteSelectSqlBlockProps) {
  const { t } = useTranslation();
  const { theme } = useTheme();
  const parsed = useMemo(() => parseExecuteSelectResponse(responseData), [responseData]);
  const [collapsed, setCollapsed] = useState(true);
  const [viewMode, setViewMode] = useState<'table' | 'json'>('table');
  const [activeResultIndex, setActiveResultIndex] = useState(0);

  useEffect(() => {
    setViewMode('table');
    setActiveResultIndex(0);
  }, [responseData]);

  const activeResultSet = parsed.resultSets[activeResultIndex] ?? null;
  const previewRows = activeResultSet?.rows.slice(0, PREVIEW_ROW_LIMIT) ?? [];
  const isHeaderError = responseError || !parsed.success;

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
          {toolName}
        </span>
        <span className={cn(TOOL_CARD_META_CLASSNAME, 'shrink-0')}>
          {parsed.resultSets.length} result{parsed.resultSets.length === 1 ? '' : 's'}
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
            <div className={`${sectionShellClassName} px-2 py-2 text-[11px]`}>
              <div className="mb-2 flex items-center gap-2">
                {viewMode === 'table' && parsed.resultSets.length > 1 ? (
                  <div className="flex flex-wrap items-center gap-1.5">
                    {parsed.resultSets.map((resultSet, index) => (
                      <button
                        key={resultSet.key}
                        type="button"
                        onClick={() => setActiveResultIndex(index)}
                        className={cn(
                          'rounded border px-2 py-1 text-[10px] transition-colors',
                          index === activeResultIndex
                            ? 'theme-text-primary theme-bg-main theme-border'
                            : 'theme-text-secondary hover:bg-black/5 dark:hover:bg-white/5 theme-border'
                        )}
                      >
                        {`${t(I18N_KEYS.COMMON.RESULT)} ${index + 1}`}
                      </button>
                    ))}
                  </div>
                ) : (
                  <div />
                )}

                <div className="ml-auto flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => setViewMode('table')}
                    className={cn(
                      'rounded border px-2 py-1 text-[10px] transition-colors',
                      viewMode === 'table'
                        ? 'theme-text-primary theme-bg-main theme-border'
                        : 'theme-text-secondary hover:bg-black/5 dark:hover:bg-white/5 theme-border'
                    )}
                  >
                    {t(I18N_KEYS.AI.EXECUTE_SELECT.TABLE)}
                  </button>
                  <button
                    type="button"
                    onClick={() => setViewMode('json')}
                    className={cn(
                      'rounded border px-2 py-1 text-[10px] transition-colors',
                      viewMode === 'json'
                        ? 'theme-text-primary theme-bg-main theme-border'
                        : 'theme-text-secondary hover:bg-black/5 dark:hover:bg-white/5 theme-border'
                    )}
                  >
                    {t(I18N_KEYS.AI.EXECUTE_SELECT.JSON)}
                  </button>
                </div>
              </div>

              {viewMode === 'table' ? (
                activeResultSet ? (
                  <>
                    <LightDataTable
                      headers={activeResultSet.headers}
                      rows={previewRows}
                      alwaysShowActions
                      headerStart={(
                        <div className="flex flex-wrap items-center gap-2">
                          <span>
                            {t(I18N_KEYS.AI.EXECUTE_SELECT.SUMMARY, {
                              rowCount: activeResultSet.rowCount,
                              columnCount: activeResultSet.columnCount,
                            })}
                          </span>
                          {(activeResultSet.truncated || activeResultSet.rowCount > PREVIEW_ROW_LIMIT) && (
                            <span className="text-amber-600 dark:text-amber-400">
                              {t(I18N_KEYS.AI.EXECUTE_SELECT.PREVIEW_LIMIT_HINT, {
                                count: PREVIEW_ROW_LIMIT,
                              })}
                            </span>
                          )}
                          {activeResultSet.limitApplied && (
                            <span className="text-amber-600 dark:text-amber-400">
                              {t(I18N_KEYS.AI.EXECUTE_SELECT.LIMIT_APPLIED_HINT)}
                            </span>
                          )}
                        </div>
                      )}
                    />
                  </>
                ) : (
                  <div className="rounded border theme-border px-3 py-4 text-center theme-text-secondary">
                    {t(I18N_KEYS.AI.EXECUTE_SELECT.NO_RESULT_SET)}
                  </div>
                )
              ) : (
                <div className="max-h-[320px] overflow-auto">
                  <SyntaxHighlighter {...highlighterProps}>
                    {parsed.prettyJson || TOOL_RUN_EMPTY_PLACEHOLDER}
                  </SyntaxHighlighter>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
