import { useTranslation } from 'react-i18next';
import { Loader2 } from 'lucide-react';
import { formatParameters } from './formatParameters';

export interface ToolRunExecutingProps {
  toolName: string;
  parametersData: string;
}

/**
 * Renders a tool call that is currently executing (arguments complete, waiting for result).
 */
export function ToolRunExecuting({ toolName, parametersData }: ToolRunExecutingProps) {
  const { t } = useTranslation();
  const { formattedParameters, isParametersJson } = formatParameters(parametersData);

  return (
    <div className="mb-2 text-xs theme-text-secondary">
      <div className="w-full py-1.5 flex items-center gap-2 text-left rounded theme-text-primary">
        <Loader2 className="w-3 h-3 animate-spin" />
        <span className="font-medium">{toolName}</span>
        <span className="text-xs opacity-70">{t('ai.toolRun.executing')}</span>
      </div>
      {isParametersJson && (
        <details className="mt-1 p-2 rounded theme-bg-tertiary cursor-pointer">
          <summary className="text-xs font-medium opacity-70">
            {t('ai.toolRun.parameters')}
          </summary>
          <pre className="text-xs opacity-80 whitespace-pre-wrap break-words mt-1">
            {formattedParameters}
          </pre>
        </details>
      )}
    </div>
  );
}
