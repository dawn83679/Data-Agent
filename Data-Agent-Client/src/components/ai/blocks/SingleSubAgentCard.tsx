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
  const glassStyle =
    theme === 'dark'
      ? isError
        ? {
            background: 'linear-gradient(135deg, rgba(239,68,68,0.16), rgba(127,29,29,0.08) 34%, rgba(30,33,43,0.96) 100%)',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.06), 0 12px 28px rgba(2,6,23,0.30)',
            backdropFilter: 'blur(10px)',
            WebkitBackdropFilter: 'blur(10px)',
          }
        : isExplorer
          ? {
              background: 'linear-gradient(135deg, rgba(34,211,238,0.18), rgba(14,116,144,0.08) 32%, rgba(30,33,43,0.96) 100%)',
              boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.06), 0 12px 28px rgba(8,47,73,0.24)',
              backdropFilter: 'blur(10px)',
              WebkitBackdropFilter: 'blur(10px)',
            }
          : {
              background: 'linear-gradient(135deg, rgba(168,85,247,0.18), rgba(109,40,217,0.08) 32%, rgba(30,33,43,0.96) 100%)',
              boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.06), 0 12px 28px rgba(59,7,100,0.22)',
              backdropFilter: 'blur(10px)',
              WebkitBackdropFilter: 'blur(10px)',
            }
      : isError
        ? {
            background: 'linear-gradient(135deg, rgba(251,113,133,0.14), rgba(255,255,255,0.94) 38%, rgba(255,255,255,0.98) 100%)',
            boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.92), 0 10px 24px rgba(244,63,94,0.08)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
          }
        : isExplorer
          ? {
              background: 'linear-gradient(135deg, rgba(59,130,246,0.14), rgba(125,211,252,0.12) 24%, rgba(255,255,255,0.96) 56%, rgba(248,252,255,0.98) 100%)',
              boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.96), 0 12px 28px rgba(37,99,235,0.08)',
              backdropFilter: 'blur(8px)',
              WebkitBackdropFilter: 'blur(8px)',
            }
          : {
              background: 'linear-gradient(135deg, rgba(99,102,241,0.14), rgba(196,181,253,0.14) 24%, rgba(255,255,255,0.96) 56%, rgba(250,248,255,0.98) 100%)',
              boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.96), 0 12px 28px rgba(99,102,241,0.08)',
              backdropFilter: 'blur(8px)',
              WebkitBackdropFilter: 'blur(8px)',
            };
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
          'mb-2 w-full rounded-lg border overflow-hidden text-left transition-all hover:-translate-y-px hover:brightness-[1.02]',
          borderColor,
          bgColor,
        )}
        style={glassStyle}
      >
        {content}
      </button>
    );
  }

  return <div className={cn('mb-2 rounded-lg border overflow-hidden', borderColor, bgColor)} style={glassStyle}>{content}</div>;
}
