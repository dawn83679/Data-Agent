import { useEffect, useMemo, useRef, useState } from 'react';
import * as echarts from 'echarts';
import type { ECharts, EChartsOption } from 'echarts';
import { AlertTriangle, CheckCircle, ChevronDown, ChevronRight, Download, Copy, Check } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAIAssistantContext } from '../AIAssistantContext';
import { GenericToolRun } from './GenericToolRun';
import { formatParameters } from './formatParameters';
import { useTheme } from '../../../hooks/useTheme';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { COPY_FEEDBACK_DELAY_MS } from '../../../constants/timing';

interface ChartToolBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  responseError: boolean;
  toolCallId?: string;
  allowAutoRetry?: boolean;
}

interface AgentToolResultWrapper {
  success?: boolean;
  message?: string;
  result?: unknown;
}

interface ChartPayload {
  chartType?: string;
  option?: Record<string, unknown>;
  description?: string;
  parseError?: string;
}

const MAX_AUTO_RETRY = 1;
const autoRetryCountByKey = new Map<string, number>();

function parseJsonSafe<T>(raw: string): T | null {
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function parseChartParams(parametersData: string): { chartType?: string; optionJson?: string; description?: string } {
  const parsed = parseJsonSafe<Record<string, unknown>>(parametersData);
  if (!parsed || typeof parsed !== 'object') {
    return {};
  }
  return {
    chartType: typeof parsed.chartType === 'string' ? parsed.chartType : undefined,
    optionJson: typeof parsed.optionJson === 'string' ? parsed.optionJson : undefined,
    description: typeof parsed.description === 'string' ? parsed.description : undefined,
  };
}

function normalizeResultPayload(value: unknown): unknown {
  if (typeof value === 'string') {
    return parseJsonSafe<unknown>(value) ?? value;
  }
  return value;
}

function parseChartPayload(responseData: string): ChartPayload {
  if (!responseData?.trim()) {
    return { parseError: 'Empty tool response.' };
  }

  const parsed = parseJsonSafe<unknown>(responseData);
  if (!parsed || typeof parsed !== 'object') {
    return { parseError: 'Tool response is not a valid JSON object.' };
  }

  const maybeWrapper = parsed as AgentToolResultWrapper;
  if (typeof maybeWrapper.success === 'boolean') {
    if (!maybeWrapper.success) {
      return {
        parseError: typeof maybeWrapper.message === 'string'
          ? maybeWrapper.message
          : 'Chart tool execution failed.',
      };
    }
  }

  const payload = normalizeResultPayload(
    Object.prototype.hasOwnProperty.call(maybeWrapper, 'result') ? maybeWrapper.result : parsed
  );

  if (!payload || typeof payload !== 'object') {
    return { parseError: 'Chart payload is missing.' };
  }

  const payloadObj = payload as Record<string, unknown>;
  const optionRaw = payloadObj.option;
  const optionObj = typeof optionRaw === 'string'
    ? parseJsonSafe<Record<string, unknown>>(optionRaw)
    : (optionRaw as Record<string, unknown> | undefined);

  if (!optionObj || typeof optionObj !== 'object' || Array.isArray(optionObj)) {
    return { parseError: 'Chart payload option is invalid or missing.' };
  }

  return {
    chartType: typeof payloadObj.chartType === 'string' ? payloadObj.chartType : undefined,
    option: optionObj,
    description: typeof payloadObj.description === 'string' ? payloadObj.description : undefined,
  };
}

function buildRetryKey(toolCallId: string | undefined, toolName: string, parametersData: string): string {
  if (toolCallId && toolCallId.trim() !== '') {
    return `${toolName}:${toolCallId}`;
  }
  return `${toolName}:${parametersData.slice(0, 160)}`;
}

function buildAutoFeedbackMessage(
  toolName: string,
  chartType: string | undefined,
  optionJson: string,
  reason: string
): string {
  return [
    '[Auto Feedback] Chart rendering failed on frontend.',
    `Tool: ${toolName}`,
    `Chart Type: ${chartType ?? 'UNKNOWN'}`,
    `Error: ${reason}`,
    'Please call renderChart again with corrected optionJson that can be rendered by ECharts.',
    'Original optionJson:',
    '```json',
    optionJson,
    '```',
  ].join('\n');
}

function mergeThemeAxis(axis: unknown, isDark: boolean): unknown {
  const labelColor = isDark ? '#d1d5db' : '#374151';
  const lineColor = isDark ? 'rgba(148,163,184,0.45)' : 'rgba(100,116,139,0.45)';
  const splitLineColor = isDark ? 'rgba(148,163,184,0.22)' : 'rgba(148,163,184,0.35)';

  const mergeOne = (axisItem: unknown): unknown => {
    if (!axisItem || typeof axisItem !== 'object' || Array.isArray(axisItem)) {
      return axisItem;
    }
    const source = axisItem as Record<string, unknown>;
    const axisLabel = (source.axisLabel as Record<string, unknown> | undefined) ?? {};
    const axisLine = (source.axisLine as Record<string, unknown> | undefined) ?? {};
    const axisLineStyle = (axisLine.lineStyle as Record<string, unknown> | undefined) ?? {};
    const splitLine = (source.splitLine as Record<string, unknown> | undefined) ?? {};
    const splitLineStyle = (splitLine.lineStyle as Record<string, unknown> | undefined) ?? {};
    const nameTextStyle = (source.nameTextStyle as Record<string, unknown> | undefined) ?? {};

    return {
      ...source,
      axisLabel: { color: labelColor, ...axisLabel },
      axisLine: {
        ...axisLine,
        lineStyle: { color: lineColor, ...axisLineStyle },
      },
      splitLine: {
        ...splitLine,
        lineStyle: { color: splitLineColor, ...splitLineStyle },
      },
      nameTextStyle: { color: labelColor, ...nameTextStyle },
    };
  };

  if (Array.isArray(axis)) {
    return axis.map(mergeOne);
  }
  return mergeOne(axis);
}

function applyThemeDefaults(option: Record<string, unknown>, theme: 'light' | 'dark'): EChartsOption {
  const isDark = theme === 'dark';
  const textColor = isDark ? '#e5e7eb' : '#111827';
  const subTextColor = isDark ? '#9ca3af' : '#6b7280';

  const sourceTextStyle = (option.textStyle as Record<string, unknown> | undefined) ?? {};
  const sourceLegend = (option.legend as Record<string, unknown> | undefined) ?? {};
  const sourceLegendTextStyle = (sourceLegend.textStyle as Record<string, unknown> | undefined) ?? {};
  const sourceTitle = (option.title as Record<string, unknown> | undefined) ?? {};
  const sourceTitleTextStyle = (sourceTitle.textStyle as Record<string, unknown> | undefined) ?? {};
  const sourceTitleSubTextStyle = (sourceTitle.subtextStyle as Record<string, unknown> | undefined) ?? {};
  const sourceGrid = (option.grid as Record<string, unknown> | undefined) ?? {};

  // Detect if this is a pie chart
  const series = option.series;
  const isPieChart = Array.isArray(series) 
    ? series.some((s: unknown) => (s as Record<string, unknown>)?.type === 'pie')
    : (series as Record<string, unknown> | undefined)?.type === 'pie';

  // Calculate top padding based on title presence
  const hasTitle = sourceTitle && (sourceTitle.text || sourceTitle.subtext);
  const defaultTop = hasTitle ? 80 : 60;

  const baseConfig = {
    ...option,
    backgroundColor: option.backgroundColor ?? 'transparent',
    textStyle: {
      color: textColor,
      ...sourceTextStyle,
    },
    title: {
      left: 'center',
      top: 20,
      ...sourceTitle,
      textStyle: {
        color: textColor,
        fontSize: 14,
        fontWeight: 'normal',
        overflow: 'break',
        width: '90%',
        ...sourceTitleTextStyle,
      },
      subtextStyle: {
        color: subTextColor,
        ...sourceTitleSubTextStyle,
      },
    },
  };

  if (isPieChart) {
    // Adjust pie chart series to have better positioning
    const adjustedSeries = Array.isArray(series)
      ? series.map((s: unknown) => {
          const seriesItem = s as Record<string, unknown>;
          if (seriesItem.type === 'pie') {
            return {
              ...seriesItem,
              center: seriesItem.center ?? ['60%', '55%'],
              radius: seriesItem.radius ?? ['0%', '65%'],
              label: {
                fontSize: 12,
                ...(seriesItem.label as Record<string, unknown> | undefined),
              },
            };
          }
          return seriesItem;
        })
      : {
          ...(series as Record<string, unknown>),
          center: (series as Record<string, unknown>)?.center ?? ['60%', '55%'],
          radius: (series as Record<string, unknown>)?.radius ?? ['0%', '65%'],
          label: {
            fontSize: 12,
            ...((series as Record<string, unknown>)?.label as Record<string, unknown> | undefined),
          },
        };

    // Special handling for pie charts
    return {
      ...baseConfig,
      series: adjustedSeries,
      legend: {
        orient: 'vertical',
        left: 20,
        top: 'middle',
        itemGap: 12,
        itemWidth: 18,
        itemHeight: 12,
        ...sourceLegend,
        textStyle: {
          color: textColor,
          fontSize: 12,
          ...sourceLegendTextStyle,
        },
      },
    } as EChartsOption;
  }

  // Default handling for bar/line/scatter charts
  return {
    ...baseConfig,
    legend: {
      ...sourceLegend,
      textStyle: {
        color: textColor,
        ...sourceLegendTextStyle,
      },
    },
    grid: {
      top: defaultTop,
      left: 70,
      right: 50,
      bottom: 100,
      containLabel: true,
      ...sourceGrid,
    },
    xAxis: mergeThemeAxis(option.xAxis, isDark),
    yAxis: mergeThemeAxis(option.yAxis, isDark),
  } as EChartsOption;
}

function getLocalizedChartTypeLabel(chartType: string | undefined, t: (key: string) => string): string {
  const normalized = (chartType ?? '').trim().toUpperCase();
  switch (normalized) {
    case 'LINE':
      return t(I18N_KEYS.AI.CHART_TYPE.LINE);
    case 'BAR':
      return t(I18N_KEYS.AI.CHART_TYPE.BAR);
    case 'PIE':
      return t(I18N_KEYS.AI.CHART_TYPE.PIE);
    case 'SCATTER':
      return t(I18N_KEYS.AI.CHART_TYPE.SCATTER);
    case 'AREA':
      return t(I18N_KEYS.AI.CHART_TYPE.AREA);
    default:
      return normalized || t(I18N_KEYS.AI.CHART_TYPE.UNKNOWN);
  }
}

export function ChartToolBlock({
  toolName,
  parametersData,
  responseData,
  responseError,
  toolCallId,
  allowAutoRetry = false,
}: ChartToolBlockProps) {
  const { submitMessage, isLoading } = useAIAssistantContext();
  const { t } = useTranslation();
  const { theme } = useTheme();
  const chartContainerRef = useRef<HTMLDivElement | null>(null);
  const chartInstanceRef = useRef<ECharts | null>(null);
  const [renderError, setRenderError] = useState<string | null>(null);
  const [collapsed, setCollapsed] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleExportChart = () => {
    const chart = chartInstanceRef.current;
    if (!chart) return;
    
    const url = chart.getDataURL({
      type: 'png',
      pixelRatio: 2,
      backgroundColor: theme === 'dark' ? '#1f2937' : '#ffffff',
    });
    
    const link = document.createElement('a');
    link.href = url;
    link.download = `chart-${Date.now()}.png`;
    link.click();
  };

  const handleCopyImage = async () => {
    const chart = chartInstanceRef.current;
    if (!chart) return;
    
    try {
      const dataUrl = chart.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: theme === 'dark' ? '#1f2937' : '#ffffff',
      });
      
      // Convert data URL to blob
      const response = await fetch(dataUrl);
      const blob = await response.blob();
      
      // Copy to clipboard
      await navigator.clipboard.write([
        new ClipboardItem({
          'image/png': blob
        })
      ]);
      
      setCopied(true);
      setTimeout(() => setCopied(false), COPY_FEEDBACK_DELAY_MS);
    } catch (error) {
      console.error('Failed to copy image:', error);
    }
  };

  const parsedParams = useMemo(() => parseChartParams(parametersData), [parametersData]);
  const chartPayload = useMemo(() => parseChartPayload(responseData), [responseData]);

  const effectiveOptionJson = useMemo(() => {
    if (parsedParams.optionJson && parsedParams.optionJson.trim() !== '') {
      return parsedParams.optionJson;
    }
    if (chartPayload.option) {
      return JSON.stringify(chartPayload.option, null, 2);
    }
    return responseData;
  }, [parsedParams.optionJson, chartPayload.option, responseData]);

  useEffect(() => {
    if (collapsed || !chartPayload.option || chartPayload.parseError) {
      return undefined;
    }

    const container = chartContainerRef.current;
    if (!container) {
      return undefined;
    }

    try {
      const chart = chartInstanceRef.current ?? echarts.init(container);
      chartInstanceRef.current = chart;
      chart.setOption(applyThemeDefaults(chartPayload.option, theme), true);
      chart.resize();
      setRenderError(null);

      const onResize = () => chart.resize();
      window.addEventListener('resize', onResize);
      return () => {
        window.removeEventListener('resize', onResize);
      };
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Unknown ECharts rendering error.';
      setRenderError(message);
      return undefined;
    }
  }, [chartPayload.option, chartPayload.parseError, collapsed, theme]);

  useEffect(() => {
    if (!collapsed) {
      return;
    }
    chartInstanceRef.current?.dispose();
    chartInstanceRef.current = null;
  }, [collapsed]);

  useEffect(() => {
    return () => {
      chartInstanceRef.current?.dispose();
      chartInstanceRef.current = null;
    };
  }, []);

  const autoRetryReason = responseError
    ? ((responseData ?? '').trim() || chartPayload.parseError || 'Backend tool execution failed.')
    : (renderError ?? chartPayload.parseError);

  const retryKey = useMemo(
    () => buildRetryKey(toolCallId, toolName, parametersData),
    [toolCallId, toolName, parametersData]
  );

  useEffect(() => {
    if (!allowAutoRetry || !autoRetryReason || isLoading) {
      return;
    }

    const current = autoRetryCountByKey.get(retryKey) ?? 0;
    if (current >= MAX_AUTO_RETRY) {
      return;
    }
    autoRetryCountByKey.set(retryKey, current + 1);
    if (autoRetryCountByKey.size > 400) {
      autoRetryCountByKey.clear();
    }

    const feedback = buildAutoFeedbackMessage(
      toolName,
      parsedParams.chartType ?? chartPayload.chartType,
      effectiveOptionJson,
      autoRetryReason
    );

    void submitMessage(feedback);
  }, [
    allowAutoRetry,
    autoRetryReason,
    isLoading,
    retryKey,
    toolName,
    parsedParams.chartType,
    chartPayload.chartType,
    effectiveOptionJson,
    submitMessage,
  ]);

  const retryCount = autoRetryCountByKey.get(retryKey) ?? 0;
  const hasFatalError = Boolean(responseError || renderError || chartPayload.parseError);
  const formattedParameters = formatParameters(parametersData);
  const showFallbackJson = hasFatalError;
  const chartTypeLabel = getLocalizedChartTypeLabel(parsedParams.chartType ?? chartPayload.chartType, t);
  const chartDescription = (chartPayload.description ?? parsedParams.description ?? '').trim();

  return (
    <div className="mb-2 space-y-2 rounded border theme-border p-2">
      <button
        type="button"
        onClick={() => setCollapsed((v) => !v)}
        className="w-full flex items-center gap-2 text-left text-[11px] font-medium theme-text-primary rounded hover:bg-black/5 dark:hover:bg-white/5 px-1 py-1"
      >
        {hasFatalError ? (
          <AlertTriangle className="w-3.5 h-3.5 text-amber-500 shrink-0" />
        ) : (
          <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" />
        )}
        <span>{chartTypeLabel}</span>
        <span className="ml-auto opacity-70">
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <>
          {hasFatalError ? (
            <div className="rounded border border-amber-500/40 bg-amber-500/10 px-2 py-1 text-[11px] theme-text-primary flex items-start gap-2">
              <AlertTriangle className="w-3.5 h-3.5 mt-0.5 text-amber-500" />
              <div>
                <div>{autoRetryReason}</div>
                {retryCount >= MAX_AUTO_RETRY && (
                  <div className="opacity-80">Auto retry limit reached. Showing JSON fallback.</div>
                )}
              </div>
            </div>
          ) : (
            <>
              <div className="relative group">
                <div
                  ref={chartContainerRef}
                  className="w-full min-h-[400px] h-[450px] rounded border theme-border theme-bg-main"
                />
                <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <div className="flex bg-white dark:bg-gray-800 rounded-lg shadow-sm border theme-border overflow-hidden">
                    <button
                      type="button"
                      onClick={handleCopyImage}
                      className="p-2 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors border-r theme-border"
                      title={copied ? t(I18N_KEYS.AI.CHART_ACTIONS.COPIED) : t(I18N_KEYS.AI.CHART_ACTIONS.COPY_IMAGE)}
                    >
                      {copied ? (
                        <Check className="w-4 h-4 text-green-600 dark:text-green-400" />
                      ) : (
                        <Copy className="w-4 h-4 theme-text-primary" />
                      )}
                    </button>
                    <button
                      type="button"
                      onClick={handleExportChart}
                      className="p-2 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                      title={t(I18N_KEYS.AI.CHART_ACTIONS.DOWNLOAD_PNG)}
                    >
                      <Download className="w-4 h-4 theme-text-primary" />
                    </button>
                  </div>
                </div>
              </div>
              {chartDescription !== '' && (
                <div className="rounded border theme-border px-2 py-1.5 text-[11px] theme-text-secondary whitespace-pre-wrap">
                  {chartDescription}
                </div>
              )}
            </>
          )}

          {showFallbackJson && (
            <GenericToolRun
              toolName={toolName}
              formattedParameters={formattedParameters}
              responseData={responseData}
              responseError={true}
            />
          )}
        </>
      )}
    </div>
  );
}
