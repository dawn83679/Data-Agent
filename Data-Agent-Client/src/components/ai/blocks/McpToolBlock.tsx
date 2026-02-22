import { useState } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { CheckCircle, XCircle, ChevronDown, ChevronRight, Copy, Check } from 'lucide-react';
import { cn } from '../../../lib/utils';

export interface McpToolBlockProps {
  toolName: string;
  parametersData: string;
  responseData: string;
  responseError?: boolean;
}

/** Detect if tool is a chart/visualization tool */
function isChartTool(toolName: string): boolean {
  const lowerName = toolName.toLowerCase();
  return (
    lowerName.includes('chart') ||
    lowerName.includes('graph') ||
    lowerName.includes('diagram') ||
    lowerName.includes('visualization')
  );
}

/** Detect if tool is a database/SQL tool */
function isDatabaseTool(toolName: string): boolean {
  const lowerName = toolName.toLowerCase();
  return (
    lowerName.includes('sql') ||
    lowerName.includes('query') ||
    lowerName.includes('database') ||
    lowerName.includes('table') ||
    lowerName.includes('ddl')
  );
}

/** Detect if response contains an image URL */
function extractImageUrl(text: string): string | null {
  if (!text) return null;
  const urlMatch = text.match(/https?:\/\/[^\s<>"]+\.(png|jpg|jpeg|gif|svg|webp)/i);
  if (urlMatch) return urlMatch[0];
  
  // Also check for Aliyun OSS URLs without extension
  const ossMatch = text.match(/https?:\/\/[^\s<>"]*alipayobjects\.com[^\s<>"]*/i);
  if (ossMatch) return ossMatch[0];
  
  return null;
}

/** Detect if parameters or response contain SQL */
function extractSql(parametersData: string, responseData: string): string | null {
  // First check response data (DDL tools return SQL in response)
  if (responseData && responseData.trim()) {
    const trimmed = responseData.trim();
    // Check if response starts with SQL keywords
    if (trimmed.match(/^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|SHOW|DESCRIBE|EXPLAIN)/i)) {
      return trimmed;
    }
  }
  
  // Then check parameters
  try {
    const params = JSON.parse(parametersData);
    // Check common SQL parameter names
    const sqlKeys = ['sql', 'query', 'statement', 'sqlStatement'];
    for (const key of sqlKeys) {
      if (params[key] && typeof params[key] === 'string') {
        return params[key];
      }
    }
  } catch {
    // If not JSON, check if the whole string looks like SQL
    if (parametersData.trim().match(/^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)/i)) {
      return parametersData.trim();
    }
  }
  
  return null;
}

/** MCP Tool Block with enhanced rendering for charts and SQL */
export function McpToolBlock({
  toolName,
  parametersData,
  responseData,
  responseError = false,
}: McpToolBlockProps) {
  const [collapsed, setCollapsed] = useState(true);
  const [imageError, setImageError] = useState(false);
  const [copied, setCopied] = useState(false);
  
  const isChart = isChartTool(toolName);
  const isDatabase = isDatabaseTool(toolName);
  const imageUrl = isChart ? extractImageUrl(responseData) : null;
  const sqlQuery = isDatabase ? extractSql(parametersData, responseData) : null;

  const handleCopyLink = async () => {
    if (!imageUrl) return;
    try {
      await navigator.clipboard.writeText(imageUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy link:', err);
    }
  };

  const { formattedParameters, isParametersJson } = (() => {
    if (!parametersData?.trim()) return { formattedParameters: '', isParametersJson: false };
    try {
      const parsed = JSON.parse(parametersData);
      return { formattedParameters: JSON.stringify(parsed, null, 2), isParametersJson: true };
    } catch {
      return { formattedParameters: parametersData, isParametersJson: false };
    }
  })();

  return (
    <div className="mb-3 rounded-lg border border-gray-200 dark:border-gray-700 overflow-hidden bg-white dark:bg-gray-800 shadow-sm">
      {/* Header */}
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className={cn(
          'w-full px-4 py-3 flex items-center gap-3 text-left transition-colors',
          'hover:bg-gray-50 dark:hover:bg-gray-750',
          responseError ? 'bg-red-50 dark:bg-red-900/20' : 'bg-gray-50 dark:bg-gray-750'
        )}
      >
        {responseError ? (
          <XCircle className="w-4 h-4 text-red-500 shrink-0" aria-label="Failed" />
        ) : (
          <CheckCircle className="w-4 h-4 text-green-500 shrink-0" aria-hidden />
        )}
        <div className="flex-1 min-w-0">
          <div className="font-medium text-sm text-gray-900 dark:text-gray-100">
            {responseError ? 'Failed ' : 'Ran '}
            {toolName}
          </div>
          {responseError && (
            <div className="text-xs text-red-600 dark:text-red-400 mt-0.5">
              Tool execution failed
            </div>
          )}
        </div>
        <span className="shrink-0 text-gray-400">
          {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </span>
      </button>

      {/* Content */}
      {!collapsed && (
        <div className="px-4 py-3 space-y-4 bg-white dark:bg-gray-800">
          {/* Image Preview for Chart Tools */}
          {imageUrl && !imageError && (
            <div className="space-y-2">
              <div className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide">
                Generated Chart
              </div>
              <div className="relative rounded-lg overflow-hidden bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700">
                <img
                  src={imageUrl}
                  alt="Generated chart"
                  className="w-full h-auto"
                  onError={() => setImageError(true)}
                />
                <button
                  type="button"
                  onClick={handleCopyLink}
                  className="absolute top-2 right-2 p-2 bg-white/90 dark:bg-gray-800/90 rounded-lg shadow-sm hover:bg-white dark:hover:bg-gray-800 transition-colors"
                  title={copied ? 'Copied!' : 'Copy link'}
                >
                  {copied ? (
                    <Check className="w-4 h-4 text-green-600 dark:text-green-400" />
                  ) : (
                    <Copy className="w-4 h-4 text-gray-600 dark:text-gray-400" />
                  )}
                </button>
              </div>
            </div>
          )}

          {/* SQL Query Highlight */}
          {sqlQuery && (
            <div className="space-y-2">
              <div className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide">
                SQL Query
              </div>
              <div className="rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
                <SyntaxHighlighter
                  language="sql"
                  style={oneLight}
                  showLineNumbers={true}
                  customStyle={{
                    margin: 0,
                    padding: '1rem',
                    fontSize: '13px',
                    lineHeight: 1.5,
                    background: '#fafafa',
                  }}
                  codeTagProps={{ 
                    style: { 
                      fontFamily: '"Fira Code", "Consolas", "Monaco", monospace',
                    } 
                  }}
                  PreTag="div"
                >
                  {sqlQuery}
                </SyntaxHighlighter>
              </div>
            </div>
          )}

          {/* Parameters */}
          <div className="space-y-2">
            <div className="text-xs font-semibold text-gray-600 dark:text-gray-400 uppercase tracking-wide">
              Parameters
            </div>
            {isParametersJson ? (
              <div className="rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
                <SyntaxHighlighter
                  language="json"
                  style={oneLight}
                  showLineNumbers={false}
                  customStyle={{
                    margin: 0,
                    padding: '0.75rem',
                    fontSize: '12px',
                    lineHeight: 1.5,
                    background: '#fafafa',
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

          {/* Response */}
          {!imageUrl && !sqlQuery && (
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
      )}
    </div>
  );
}
