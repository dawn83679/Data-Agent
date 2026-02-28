import { useTranslation } from 'react-i18next';
import { Loader2 } from 'lucide-react';
import { formatParameters } from './formatParameters';
import { I18N_KEYS } from '../../../constants/i18nKeys';

export interface ToolRunExecutingProps {
  toolName: string;
  parametersData: string;
}

/**
 * Renders a tool call that is currently executing (arguments complete, waiting for result).
 */
export function ToolRunExecuting({ toolName, parametersData }: ToolRunExecutingProps) {
  const { t } = useTranslation();
  const formattedParameters = formatParameters(parametersData);

  return (
    <div className="mb-2 text-xs theme-text-secondary">
      <div className="w-full py-1.5 flex items-center gap-2 text-left rounded theme-text-primary">
        <Loader2 className="w-3 h-3 animate-spin" />
        <span className="font-medium">{toolName}</span>
        <span className="text-xs opacity-70">{t(I18N_KEYS.AI.TOOL_RUN.EXECUTING)}</span>
      </div>
      {formattedParameters && (
        <details className="mt-1 p-2 rounded theme-bg-tertiary cursor-pointer">
          <summary className="text-xs font-medium opacity-70">
            {t(I18N_KEYS.AI.TOOL_RUN.PARAMETERS)}
          </summary>
          <pre className="text-xs opacity-80 whitespace-pre-wrap break-words mt-1">
            {formattedParameters}
          </pre>
        </details>
      )}
    </div>
  );
}
