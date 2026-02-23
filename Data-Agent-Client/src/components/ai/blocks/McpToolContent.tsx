import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useTheme } from '../../../hooks/useTheme';
import { ChartDisplay } from './ChartDisplay';
import type { RenderConfig } from './McpToolConfig';

export interface McpToolContentProps {
  config: RenderConfig;
  imageUrl: string | null;
  toolName: string;
  parametersData: string;
  responseData: string;
}

/**
 * Format chart tool name to human-readable type
 * @example "generate_bar_chart" → "Bar Chart"
 */
function formatChartType(toolName: string): string {
  const type = toolName.replace(/^generate_/, '').replace(/_/g, ' ');
  return type
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Unified content component for MCP tools
 * Conditionally renders chart, parameters, and response based on config
 */
export function McpToolContent({
  config,
  imageUrl,
  toolName,
  parametersData,
  responseData,
}: McpToolContentProps) {
  const { theme } = useTheme();

  const syntaxTheme = theme === 'dark' ? oneDark : oneLight;
  const syntaxBg = theme === 'dark' ? '#1e1e1e' : '#fafafa';

  // Format parameters
  const { formattedParameters, isParametersJson } = (() => {
    if (!parametersData?.trim()) return { formattedParameters: '', isParametersJson: false };
    try {
      const parsed = JSON.parse(parametersData);
      return { formattedParameters: JSON.stringify(parsed, null, 2), isParametersJson: true };
    } catch {
      return { formattedParameters: parametersData, isParametersJson: false };
    }
  })();

  // Container class based on mode
  const containerClass = config.mode === 'minimal'
    ? 'mt-2'
    : 'px-4 py-3 space-y-4 bg-white dark:bg-gray-800';

  return (
    <div className={containerClass}>
      {/* Chart display - both modes may show chart */}
      {imageUrl && (
        <div className={config.mode === 'detailed' ? 'space-y-2' : ''}>
          {config.mode === 'detailed' && (
            <div className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide">
              Generated Chart
            </div>
          )}
          <ChartDisplay imageUrl={imageUrl} chartType={formatChartType(toolName)} />
        </div>
      )}

      {/* Parameters - detailed mode only */}
      {config.showParameters && (
        <div className="space-y-2">
          <div className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide">
            Parameters
          </div>
          {isParametersJson ? (
            <div className="rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
              <SyntaxHighlighter
                language="json"
                style={syntaxTheme}
                showLineNumbers={false}
                customStyle={{
                  margin: 0,
                  padding: '0.75rem',
                  fontSize: '12px',
                  lineHeight: 1.5,
                  background: syntaxBg,
                  maxHeight: '200px',
                }}
                codeTagProps={{
                  style: {
                    fontFamily: '"Fira Code", "Consolas", "Monaco", monospace',
                  }
                }}
                PreTag="div"
              >
                {formattedParameters}
              </SyntaxHighlighter>
            </div>
          ) : (
            <pre className="py-3 px-4 rounded-lg bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 font-mono text-xs overflow-x-auto whitespace-pre-wrap break-words max-h-[200px] overflow-y-auto">
              {formattedParameters || '(empty)'}
            </pre>
          )}
        </div>
      )}

      {/* Response - detailed mode only, when no chart */}
      {config.showResponse && !imageUrl && (
        <div className="space-y-2">
          <div className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide">
            Response
          </div>
          <pre className="py-3 px-4 rounded-lg bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 font-mono text-xs overflow-x-auto max-h-[300px] overflow-y-auto whitespace-pre-wrap break-words">
            {responseData || '(empty)'}
          </pre>
        </div>
      )}
    </div>
  );
}
