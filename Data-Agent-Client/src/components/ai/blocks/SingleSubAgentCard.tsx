import { Braces, CheckCircle, Database, Loader2, XCircle } from 'lucide-react';
import { cn } from '../../../lib/utils';
import { useTheme } from '../../../hooks/useTheme';
import { getAgentTheming } from './subAgentDataHelpers';

export interface SingleSubAgentCardProps {
  agentType: string;
  label: string;
  statusText: string;
  isComplete: boolean;
  isError: boolean;
  elapsedText?: string;
  onOpenConsole?: () => void;
}

export function SingleSubAgentCard({
  agentType,
  label,
  statusText,
  isComplete,
  isError,
  elapsedText,
  onOpenConsole,
}: SingleSubAgentCardProps) {
  const { theme } = useTheme();
  const { isExplorer, borderColor, bgColor, iconColor } = getAgentTheming(agentType, isError, theme);
  const AgentIcon = isExplorer ? Database : Braces;
  const shouldShowStatusText = !isComplete || isError;
  const content = (
    <>
      <div className="px-3 py-2 flex items-center gap-2">
        {isComplete && !isError ? (
          <CheckCircle className="w-3.5 h-3.5 text-green-500 shrink-0" />
        ) : isError ? (
          <XCircle className="w-3.5 h-3.5 text-red-500 shrink-0" />
        ) : (
          <Loader2 className={cn('w-3.5 h-3.5 shrink-0 animate-spin', iconColor)} />
        )}
        <AgentIcon className={cn('w-3.5 h-3.5 shrink-0', iconColor)} />
        <span className="text-[12px] font-medium theme-text-primary">{label}</span>
        <span className="ml-auto flex items-center gap-3 text-[11px] theme-text-secondary tabular-nums">
          {elapsedText && <span>{elapsedText}</span>}
        </span>
      </div>
      {shouldShowStatusText && (
        <div className="px-3 pb-2 -mt-0.5">
          <p className={cn('text-[11px] pl-5 whitespace-normal break-words', isComplete ? 'theme-text-primary' : 'theme-text-secondary')}>
            {statusText}
          </p>
        </div>
      )}
    </>
  );

  if (onOpenConsole) {
    return (
      <button
        type="button"
        onClick={onOpenConsole}
        className={cn(
          'mb-2 w-full rounded-lg border overflow-hidden text-left shadow-[0_1px_2px_rgba(15,23,42,0.06),0_8px_24px_rgba(15,23,42,0.04)] transition-colors hover:shadow-[0_1px_2px_rgba(15,23,42,0.08),0_10px_28px_rgba(15,23,42,0.06)]',
          borderColor,
          bgColor,
        )}
      >
        {content}
      </button>
    );
  }

  return <div className={cn('mb-2 rounded-lg border overflow-hidden shadow-[0_1px_2px_rgba(15,23,42,0.06),0_8px_24px_rgba(15,23,42,0.04)]', borderColor, bgColor)}>{content}</div>;
}
