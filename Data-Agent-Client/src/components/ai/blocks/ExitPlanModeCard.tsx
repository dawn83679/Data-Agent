import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Play, X, RotateCcw } from 'lucide-react';
import { useAIAssistantContext } from '../AIAssistantContext';
import { useWorkspaceStore } from '../../../store/workspaceStore';

export interface ExitPlanModeCardProps {
  /** The tab ID of the associated plan tab, for closing on exit. */
  planTabId?: string;
  submittedAction?: string;
}

/**
 * Chat-sidebar card showing action buttons for a plan.
 * Steps are displayed in the PlanConsole tab, not here.
 */
export function ExitPlanModeCard({ planTabId, submittedAction }: ExitPlanModeCardProps) {
  const { t } = useTranslation();
  const { submitMessage, isLoading, agentState } = useAIAssistantContext();
  const [isSubmitted, setIsSubmitted] = useState(false);

  if (submittedAction || isSubmitted) {
    return null;
  }

  const handleExecute = () => {
    if (isLoading) return;
    setIsSubmitted(true);
    agentState.setAgent('Agent');
    submitMessage(t('ai.plan.executeMessage', 'Please execute the plan above. Use todoWrite to create a todo list for progress tracking first, then execute each step sequentially.'), { agentType: 'Agent' });
  };

  const handleExit = () => {
    if (isLoading) return;
    setIsSubmitted(true);
    agentState.setAgent('Agent');
    // Close the plan tab
    if (planTabId) {
      useWorkspaceStore.getState().closeTab(planTabId);
    }
  };

  const handleContinue = () => {
    if (isLoading) return;
    setIsSubmitted(true);
    submitMessage(t('ai.plan.continueMessage', 'I have additional requirements for this plan.'));
  };

  return (
    <div className="p-3 rounded-lg border border-amber-300 dark:border-amber-700 bg-amber-50/50 dark:bg-amber-900/10 shadow-sm flex flex-col gap-3">
      {/* Action Buttons */}
      <div className="flex gap-2">
        <button
          type="button"
          onClick={handleExecute}
          disabled={isLoading}
          className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-[12px] font-medium bg-green-600 hover:bg-green-700 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Play className="w-3.5 h-3.5" />
          {t('ai.plan.executeBtn', 'Execute Plan')}
        </button>
        <button
          type="button"
          onClick={handleExit}
          disabled={isLoading}
          className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-[12px] font-medium bg-zinc-500 hover:bg-zinc-600 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <X className="w-3.5 h-3.5" />
          {t('ai.plan.exitBtn', 'Exit')}
        </button>
        <button
          type="button"
          onClick={handleContinue}
          disabled={isLoading}
          className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-[12px] font-medium bg-amber-500 hover:bg-amber-600 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          {t('ai.plan.continueBtn', 'Continue Planning')}
        </button>
      </div>
    </div>
  );
}
