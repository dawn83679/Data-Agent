import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../../lib/utils';
import type { WaitingPromptMode } from '../../../types/chat';
import { I18N_KEYS } from '../../../constants/i18nKeys';

export interface PlanningIndicatorProps {
  className?: string;
  mode?: WaitingPromptMode;
}

const PROMPT_ROTATION_MS = 5000;

export const WAITING_PROMPT_FALLBACKS: Record<WaitingPromptMode, string[]> = {
  default: [
    '正在处理你的请求...',
    '还在继续处理，请稍等...',
    '处理时间稍长，正在继续...',
    '仍在处理中...',
  ],
  answer: [
    '模型正在处理你的回答...',
    '还在继续处理你的回答，请稍等...',
    '处理时间稍长，正在继续...',
    '仍在处理中...',
  ],
};

export function getWaitingPrompt(
  mode: WaitingPromptMode,
  index: number,
  labels: string[] | undefined
): string {
  const candidates = labels?.filter((label) => label.trim() !== '');
  const prompts = candidates && candidates.length > 0 ? candidates : WAITING_PROMPT_FALLBACKS[mode];
  return prompts[Math.min(index, prompts.length - 1)];
}

/** Pulsing waiting prompt shown while waiting for AI response or during inter-block gaps. */
export function PlanningIndicator({ className, mode = 'default' }: PlanningIndicatorProps) {
  const { t } = useTranslation();
  const [promptIndex, setPromptIndex] = useState(0);
  const labels = useMemo(() => {
    const key = mode === 'answer'
      ? I18N_KEYS.AI.WAITING.ANSWER
      : I18N_KEYS.AI.WAITING.DEFAULT;
    const value = t(key, { returnObjects: true }) as unknown;
    return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : undefined;
  }, [mode, t]);
  const label = getWaitingPrompt(mode, promptIndex, labels);

  useEffect(() => {
    setPromptIndex(0);
    const timer = window.setInterval(() => {
      setPromptIndex((current) => current + 1);
    }, PROMPT_ROTATION_MS);
    return () => window.clearInterval(timer);
  }, [mode]);

  return (
    <div className={cn('text-xs opacity-70 theme-text-secondary', className)}>
      <span className="inline-flex items-center font-medium leading-4 animate-pulse">
        {label}
      </span>
    </div>
  );
}
