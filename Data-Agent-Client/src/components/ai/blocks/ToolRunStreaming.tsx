import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../../constants/i18nKeys';

export interface ToolRunStreamingProps {
  toolName: string;
  partialArguments: string;
}

/**
 * Renders a tool call with streaming arguments (typing effect).
 */
export function ToolRunStreaming({ toolName, partialArguments }: ToolRunStreamingProps) {
  const { t } = useTranslation();

  return (
    <div className="mb-2 text-xs theme-text-secondary">
      <div className="w-full py-1.5 flex items-start gap-2 text-left rounded">
        <span className="font-medium animate-pulse">{toolName}</span>
        <span className="text-xs opacity-70">{t(I18N_KEYS.AI.TOOL_RUN.STREAMING_ARGUMENTS)}</span>
      </div>
      {partialArguments && (
        <div className="mt-1 p-2 rounded theme-bg-tertiary">
          <pre className="text-xs opacity-80 whitespace-pre-wrap break-words">
            {partialArguments}
            <span className="animate-blink">▋</span>
          </pre>
        </div>
      )}
    </div>
  );
}
