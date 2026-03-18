import React from 'react';
import { cn } from '../../../lib/utils';
import type { SubAgentProgressEvent } from './subAgentTypes';
import { getNestedToolStats } from './subAgentDataHelpers';
import type { Segment } from '../messageListLib/types';

interface SubAgentProgressTimelineProps {
  progressEvents: SubAgentProgressEvent[];
  iconColor: string;
  isComplete: boolean;
  nestedToolRuns?: Segment[];
}

export const SubAgentProgressTimeline = React.memo(function SubAgentProgressTimeline({
  progressEvents,
  iconColor,
  isComplete,
  nestedToolRuns,
}: SubAgentProgressTimelineProps) {
  return (
    <div className="mt-2 mb-2">
      <p className="text-[10px] font-medium theme-text-secondary uppercase tracking-wider mb-1">Progress</p>
      <div className="flex flex-col gap-0.5">
        {progressEvents.map((evt, idx) => {
          const isLast = idx === progressEvents.length - 1;
          const isGenericProgress =
            evt.phase === 'progress' &&
            (evt.message === '正在探索数据库结构...' || evt.message === '正在分析查询需求...');
          const runningToolName = getNestedToolStats(nestedToolRuns).runningToolName;
          const displayMessage =
            !isComplete && isLast && isGenericProgress && runningToolName
              ? `Running ${runningToolName}`
              : evt.message ?? evt.phase;

          const connector = isLast ? '\u2514\u2500' : '\u251C\u2500';
          const phaseColor = evt.phase === 'error'
            ? 'text-red-500'
            : evt.phase === 'complete'
              ? 'text-green-500'
              : evt.phase === 'start'
                ? 'text-green-400'
                : iconColor;

          return (
            <div
              key={idx}
              className={cn(
                'flex items-start gap-1.5 text-[11px] font-mono',
                !isComplete && isLast ? 'animate-pulse' : ''
              )}
            >
              <span className="theme-text-secondary select-none">{connector}</span>
              <span className={cn('shrink-0', phaseColor)}>{'\u25CF'}</span>
              <span className="theme-text-primary break-words">{displayMessage}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
});
