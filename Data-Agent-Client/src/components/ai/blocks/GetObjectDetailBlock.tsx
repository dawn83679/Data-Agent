import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
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
import { connectionService } from '../../../services/connection.service';
import { parseGetObjectDetailResponse } from './getObjectDetailTypes';
import { SqlPreviewCard } from './SqlPreviewCard';

export interface GetObjectDetailBlockProps {
  toolName: string;
  formattedParameters: string;
  responseData: string;
  responseError: boolean;
}

export function GetObjectDetailBlock({
  toolName,
  formattedParameters,
  responseData,
  responseError,
}: GetObjectDetailBlockProps) {
  const { t } = useTranslation();
  const { theme } = useTheme();
  const parsed = useMemo(() => parseGetObjectDetailResponse(responseData), [responseData]);
  const [collapsed, setCollapsed] = useState(true);
  const [viewMode, setViewMode] = useState<'ddl' | 'json'>('ddl');
  const [activeItemIndex, setActiveItemIndex] = useState(0);

  useEffect(() => {
    setViewMode('ddl');
    setActiveItemIndex(0);
  }, [responseData]);

  const activeItem = parsed.items[activeItemIndex] ?? null;
  const isHeaderError = responseError;
  const { data: connections = [] } = useQuery({
    queryKey: ['connections'],
    queryFn: () => connectionService.getConnections(),
    staleTime: 5 * 60 * 1000,
  });

  const itemLabels = useMemo(() => {
    const counts = new Map<string, number>();
    const seen = new Map<string, number>();
    parsed.items.forEach((item, index) => {
      const base = item.objectName.trim() || t(I18N_KEYS.AI.GET_OBJECT_DETAIL.OBJECT_FALLBACK, { count: index + 1 });
      counts.set(base, (counts.get(base) ?? 0) + 1);
    });

    return parsed.items.map((item, index) => {
      const base = item.objectName.trim() || t(I18N_KEYS.AI.GET_OBJECT_DETAIL.OBJECT_FALLBACK, { count: index + 1 });
      const duplicateTotal = counts.get(base) ?? 0;
      if (duplicateTotal <= 1) {
        return base;
      }
      seen.set(base, (seen.get(base) ?? 0) + 1);
      const duplicateIndex = seen.get(base) ?? 1;
      if (item.objectType?.trim()) {
        return `${base} (${item.objectType})`;
      }
      return `${base} #${duplicateIndex}`;
    });
  }, [parsed.items, t]);

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

  const connectionNameById = useMemo(
    () => new Map(connections.map((connection) => [connection.id, connection.name])),
    [connections]
  );

  const activeItemHeadline = useMemo(() => {
    if (!activeItem) {
      return '';
    }
    const objectLabel = activeItem.objectName.trim() || t(I18N_KEYS.AI.GET_OBJECT_DETAIL.OBJECT_FALLBACK, { count: activeItemIndex + 1 });
    const connectionName = activeItem.connectionId != null
      ? connectionNameById.get(activeItem.connectionId) || `#${activeItem.connectionId}`
      : undefined;
    const path = [
      connectionName,
      activeItem.databaseName,
      activeItem.schemaName,
      objectLabel,
    ].filter((part): part is string => !!part && part.trim() !== '').join('>');
    const prefix = activeItem.objectType?.trim() ? `${activeItem.objectType}:` : '';
    return `${prefix}${path}`;
  }, [activeItem, activeItemIndex, connectionNameById, t]);

  const activeItemMeta = activeItem ? [
    activeItem.rowCount != null ? t(I18N_KEYS.AI.GET_OBJECT_DETAIL.ROW_COUNT, { count: activeItem.rowCount }) : undefined,
    t(I18N_KEYS.AI.GET_OBJECT_DETAIL.INDEX_COUNT, { count: activeItem.indexesCount }),
  ].filter((part): part is string => !!part && part.trim() !== '') : [];

  return (
    <div
      className={cn(
        'mb-2 text-xs rounded transition-colors',
        collapsed ? 'opacity-70 theme-text-secondary' : 'opacity-100 theme-text-primary'
      )}
    >
      <button
        type="button"
        onClick={() => setCollapsed((current) => !current)}
        className="w-full py-1.5 flex items-center gap-2 text-left rounded transition-colors theme-text-primary hover:bg-[color:var(--bg-popup)]/55"
      >
        {isHeaderError ? (
          <XCircle className="w-3.5 h-3.5 text-red-500 shrink-0" aria-label="Failed" />
        ) : (
          <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" aria-hidden />
        )}
        <span className="font-medium">
          {isHeaderError ? TOOL_RUN_LABEL_FAILED : TOOL_RUN_LABEL_RAN}
          {toolName}
        </span>
        <span className={cn('ml-auto shrink-0', collapsed ? 'opacity-60' : 'opacity-80')}>
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
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
            <div className={`${sectionShellClassName} px-2 py-2 text-[11px]`}>
              <div className="mb-2 flex items-center gap-2">
                {viewMode === 'ddl' && parsed.items.length > 1 ? (
                  <div className="flex flex-wrap items-center gap-1.5">
                    {parsed.items.map((item, index) => (
                      <button
                        key={item.key}
                        type="button"
                        onClick={() => setActiveItemIndex(index)}
                        className={cn(
                          'max-w-[220px] truncate rounded border px-2 py-1 text-[10px] transition-colors',
                          index === activeItemIndex
                            ? 'theme-text-primary theme-bg-main theme-border'
                            : 'theme-text-secondary hover:bg-black/5 dark:hover:bg-white/5 theme-border'
                        )}
                        title={itemLabels[index] ?? ''}
                      >
                        {itemLabels[index] ?? ''}
                      </button>
                    ))}
                  </div>
                ) : (
                  <div />
                )}

                <div className="ml-auto flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => setViewMode('ddl')}
                    className={cn(
                      'rounded border px-2 py-1 text-[10px] transition-colors',
                      viewMode === 'ddl'
                        ? 'theme-text-primary theme-bg-main theme-border'
                        : 'theme-text-secondary hover:bg-black/5 dark:hover:bg-white/5 theme-border'
                    )}
                  >
                    {t(I18N_KEYS.AI.GET_OBJECT_DETAIL.DDL)}
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
                    {t(I18N_KEYS.AI.GET_OBJECT_DETAIL.JSON)}
                  </button>
                </div>
              </div>

              {viewMode === 'ddl' ? (
                activeItem ? (
                  activeItem.success && activeItem.ddl ? (
                    <SqlPreviewCard
                      sql={activeItem.ddl}
                      wrapLongLines
                      headerStart={(
                        <div className="flex min-w-0 flex-wrap items-center gap-2">
                          <span className="font-medium theme-text-primary">{activeItemHeadline}</span>
                          {activeItemMeta.map((part) => (
                            <span key={part} className="theme-text-secondary">
                              {part}
                            </span>
                          ))}
                        </div>
                      )}
                    />
                  ) : (
                    <div className="rounded border border-red-500/30 bg-red-500/5 px-3 py-3">
                      <div className="mb-1 flex min-w-0 flex-wrap items-center gap-2">
                        <span className="font-medium theme-text-primary">{activeItemHeadline}</span>
                        {activeItemMeta.map((part) => (
                          <span key={part} className="theme-text-secondary">
                            {part}
                          </span>
                        ))}
                      </div>
                      <div className="text-red-500">
                        {activeItem.error || t(I18N_KEYS.AI.GET_OBJECT_DETAIL.DETAIL_FAILED)}
                      </div>
                    </div>
                  )
                ) : (
                  <div className="rounded border theme-border px-3 py-4 text-center theme-text-secondary">
                    {t(I18N_KEYS.AI.GET_OBJECT_DETAIL.NO_DETAILS)}
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
