import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, Clock3, ShieldCheck, XCircle } from 'lucide-react';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { SqlCodeBlock } from '../../common/SqlCodeBlock';
import { useAIAssistantContext } from '../AIAssistantContext';
import { connectionService } from '../../../services/connection.service';
import { permissionService } from '../../../services/permission.service';
import {
  PermissionGrantCoverage,
  PermissionGrantPreset,
  PermissionScopeType,
  type PermissionGrantOption,
} from '../../../types/permission';
import {
  permissionCoverageLabel,
  permissionCycleCoverage,
  permissionCycleScope,
  permissionFindGrantOption,
  permissionGrantCoverageOptionsForScope,
  permissionGrantPresetLabel,
  permissionScopeDescription,
} from '../../../lib/permissionDisplay';
import { ExecuteNonSelectToolStatus, type ExecuteNonSelectToolResultPayload } from './executeNonSelectTypes';

export interface WriteExecutionConfirmCardProps {
  payload: ExecuteNonSelectToolResultPayload;
}

const OPTION_CARD_CLASS = 'flex min-w-0 flex-col gap-3 p-3';
const CLICKABLE_OPTION_CLASS = [
  'cursor-pointer transition-colors hover:bg-black/5 dark:hover:bg-white/5',
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent-blue)] focus-visible:ring-inset',
  'disabled:cursor-not-allowed disabled:opacity-60',
].join(' ');
const INLINE_TEXT_TOGGLE_CLASS = [
  'inline rounded-sm font-semibold text-[var(--accent-blue)] underline decoration-dotted underline-offset-2',
  'transition-opacity hover:opacity-75',
  'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-[var(--accent-blue)] focus-visible:ring-offset-2',
  'disabled:cursor-default disabled:no-underline disabled:opacity-100',
].join(' ');

export function WriteExecutionConfirmCard({ payload }: WriteExecutionConfirmCardProps) {
  const { t } = useTranslation();
  const { submitMessage, isLoading } = useAIAssistantContext();
  const [isProcessing, setIsProcessing] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [supplementaryInput, setSupplementaryInput] = useState('');
  const confirmation = payload.confirmation;
  const grantOptions = useMemo(() => confirmation?.availableGrantOptions ?? [], [confirmation?.availableGrantOptions]);
  const [selectedScope, setSelectedScope] = useState<PermissionScopeType>(defaultScope(grantOptions));
  const [selectedCoverage, setSelectedCoverage] = useState<PermissionGrantCoverage>(
    defaultCoverage(grantOptions, defaultScope(grantOptions))
  );

  const connectionId = confirmation?.connectionId ?? 0;
  const { data: connection } = useQuery({
    queryKey: ['connection', connectionId],
    queryFn: () => connectionService.getConnectionById(connectionId),
    enabled: connectionId > 0,
    staleTime: 5 * 60 * 1000,
  });

  const canConfirm = !!confirmation && confirmation.conversationId > 0;
  const selectedGrant = useMemo(() => {
    return permissionFindGrantOption(grantOptions, selectedScope, selectedCoverage);
  }, [grantOptions, selectedCoverage, selectedScope]);

  useEffect(() => {
    const nextScope = defaultScope(grantOptions);
    setSelectedScope(nextScope);
    setSelectedCoverage(defaultCoverage(grantOptions, nextScope));
  }, [grantOptions]);

  const approveWriteExecution = async (grantOption: PermissionGrantOption | null) => {
    if (!confirmation || !canConfirm || isProcessing || isLoading) return;
    setIsProcessing(true);
    try {
      await permissionService.approveWriteExecution({
        conversationId: confirmation.conversationId,
        connectionId: confirmation.connectionId,
        catalogName: normalizedCatalogName(grantOption, confirmation.databaseName),
        schemaName: normalizedSchemaName(grantOption, confirmation.schemaName),
        sql: confirmation.sql,
        scopeType: grantOption?.scopeType ?? null,
        grantPreset: grantOption?.grantPreset ?? null,
      });

      await submitMessage(buildFollowupMessage(t, grantOption));
      setIsSubmitted(true);
    } catch {
      await submitMessage(t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.APPROVE_FAILED_MESSAGE));
    } finally {
      setIsProcessing(false);
    }
  };

  const handleExecuteOnce = async () => {
    await approveWriteExecution(null);
  };

  const handleSaveDefaultAllow = async () => {
    if (!selectedGrant) return;
    await approveWriteExecution(selectedGrant);
  };

  const handleReject = async () => {
    if (isProcessing || isLoading) return;
    setIsProcessing(true);
    try {
      await submitMessage(buildRejectMessage(t, supplementaryInput));
      setIsSubmitted(true);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleToggleScope = () => {
    if (isProcessing || isLoading) return;
    const nextScope = permissionCycleScope(grantOptions, selectedScope);
    const nextGrant = permissionFindGrantOption(grantOptions, nextScope, selectedCoverage);
    setSelectedScope(nextScope);
    if (nextGrant) {
      return;
    }
    setSelectedCoverage(defaultCoverage(grantOptions, nextScope));
  };

  const handleToggleCoverage = () => {
    if (isProcessing || isLoading) return;
    const nextCoverage = permissionCycleCoverage(grantOptions, selectedCoverage, selectedScope);
    setSelectedCoverage(nextCoverage);
  };

  const handleOptionKeyDown = (
    event: React.KeyboardEvent<HTMLElement>,
    action: () => Promise<void>,
    disabled: boolean
  ) => {
    if (disabled) return;
    if (event.key !== 'Enter' && event.key !== ' ') return;
    event.preventDefault();
    void action();
  };

  if (!confirmation || isSubmitted || payload.status !== ExecuteNonSelectToolStatus.REQUIRES_CONFIRMATION) {
    return null;
  }

  const pathParts = [connection?.name || `#${connectionId}`];
  if (confirmation.databaseName) pathParts.push(confirmation.databaseName);
  if (confirmation.schemaName) pathParts.push(confirmation.schemaName);
  const connectionPath = pathParts.join(' -> ');
  const target = [confirmation.databaseName, confirmation.schemaName]
    .filter((value): value is string => !!value)
    .join('.');

  const interactiveDisabled = isProcessing || isLoading;
  const defaultAllowDisabled = !canConfirm || interactiveDisabled || !selectedGrant;
  const canToggleScope = !interactiveDisabled && new Set(grantOptions.map((option) => option.scopeType)).size > 1;
  const canToggleCoverage = !interactiveDisabled
    && permissionGrantCoverageOptionsForScope(grantOptions, selectedScope).length > 1;
  const selectedScopeLabel = permissionScopeDescription(t, selectedScope);
  const selectedCoverageLabel = permissionCoverageLabel(
    t,
    selectedCoverage,
    confirmation.databaseName,
    confirmation.schemaName
  );
  const selectedScopeHint = permissionScopeHint(t, selectedScope);
  const selectedCoverageHint = permissionCoverageHint(
    t,
    selectedCoverage,
    confirmation.databaseName,
    confirmation.schemaName
  );

  return (
    <div className="mb-2 flex flex-col gap-3 rounded-lg border theme-border theme-bg-main p-4 shadow-sm">
      <div className="flex items-center gap-2 font-medium text-amber-600 dark:text-amber-500">
        <AlertCircle className="h-4 w-4" />
        <span className="text-[11px] uppercase tracking-wide">{t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.LABEL_ACTION)}</span>
      </div>

      {payload.message && (
        <p className="whitespace-pre-wrap text-[13px] theme-text-primary">{payload.message}</p>
      )}

      <div className="overflow-hidden rounded border theme-border theme-bg-main">
        <div className="flex justify-between border-b theme-border theme-bg-panel px-2 py-1 text-xs font-medium">
          <span>{t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.SQL_PREVIEW_LABEL)}</span>
          <span className="font-mono opacity-60">{connectionPath}</span>
        </div>
        <SqlCodeBlock variant="compact" sql={confirmation.sqlPreview} wrapLongLines={true} />
      </div>

      {target && (
        <p className="text-[12px] theme-text-secondary">
          {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.TARGET_LABEL)}:{' '}
          <code className="font-mono theme-text-primary">{target}</code>
        </p>
      )}

      <div className="overflow-hidden rounded-lg border theme-border theme-bg-panel">
        <div className="flex flex-col divide-y theme-border">
        <section
          role="button"
          tabIndex={interactiveDisabled ? -1 : 0}
          onClick={() => {
            void handleExecuteOnce();
          }}
          onKeyDown={(event) => handleOptionKeyDown(event, handleExecuteOnce, !canConfirm || interactiveDisabled)}
          className={`${OPTION_CARD_CLASS} ${CLICKABLE_OPTION_CLASS}`}
        >
          <div className="flex flex-1 flex-col gap-3">
            <div className="flex items-start gap-2.5">
              <div className="rounded-full bg-slate-500/10 p-1.5 text-slate-600 dark:bg-white/10 dark:text-slate-200">
                <Clock3 className="h-4 w-4" />
              </div>
              <div className="min-w-0">
                <div className="text-[13px] font-semibold theme-text-primary">
                  {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.EXECUTE_ONCE_LABEL)}
                </div>
                <p className="mt-1 text-[12px] leading-5 theme-text-secondary">
                  {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.EXECUTE_ONCE_HINT)}
                </p>
              </div>
            </div>
          </div>
        </section>

        <section
          role="button"
          tabIndex={defaultAllowDisabled ? -1 : 0}
          onClick={() => {
            void handleSaveDefaultAllow();
          }}
          onKeyDown={(event) => handleOptionKeyDown(event, handleSaveDefaultAllow, defaultAllowDisabled)}
          className={`${OPTION_CARD_CLASS} ${CLICKABLE_OPTION_CLASS}`}
        >
          <div className="flex flex-1 flex-col gap-3">
            <div className="flex items-start gap-2.5">
              <div className="rounded-full bg-[var(--accent-blue)]/10 p-1.5 text-[var(--accent-blue)]">
                <ShieldCheck className="h-4 w-4" />
              </div>
              <div className="min-w-0">
                <div className="text-[13px] font-semibold theme-text-primary">
                  {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.SAVE_AND_EXECUTE_LABEL)}
                </div>
                <p className="mt-1 text-[12px] leading-5 theme-text-secondary">
                  {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.DEFAULT_ALLOW_SENTENCE_PREFIX)}{' '}
                  <button
                    type="button"
                    onClick={(event) => {
                      event.stopPropagation();
                      handleToggleScope();
                    }}
                    disabled={!canToggleScope}
                    className={INLINE_TEXT_TOGGLE_CLASS}
                    title={selectedScopeHint}
                  >
                    {selectedScopeLabel}
                  </button>{' '}
                  {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.DEFAULT_ALLOW_SENTENCE_MIDDLE)}{' '}
                  <button
                    type="button"
                    onClick={(event) => {
                      event.stopPropagation();
                      handleToggleCoverage();
                    }}
                    disabled={!canToggleCoverage}
                    className={INLINE_TEXT_TOGGLE_CLASS}
                    title={selectedCoverageHint}
                  >
                    {selectedCoverageLabel}
                  </button>{' '}
                  {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.DEFAULT_ALLOW_SENTENCE_SUFFIX)}
                </p>
                <p className="mt-1 text-[11px] font-medium text-[var(--accent-blue)]/80">
                  {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.EDITABLE_HINT)}
                </p>
              </div>
            </div>
          </div>
        </section>

        <section
          role="button"
          tabIndex={interactiveDisabled ? -1 : 0}
          onClick={() => {
            void handleReject();
          }}
          onKeyDown={(event) => handleOptionKeyDown(event, handleReject, interactiveDisabled)}
          className={`${OPTION_CARD_CLASS} ${CLICKABLE_OPTION_CLASS}`}
        >
          <div className="flex flex-1 flex-col gap-3">
            <div className="flex items-start gap-2.5">
              <div className="rounded-full bg-red-500/10 p-1.5 text-red-500 dark:bg-red-500/15 dark:text-red-300">
                <XCircle className="h-4 w-4" />
              </div>
              <div className="min-w-0">
                <div className="text-[13px] font-semibold theme-text-primary">
                  {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.REJECT_LABEL)}
                </div>
                <p className="mt-1 text-[12px] leading-5 theme-text-secondary">
                  {t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.REJECT_INPUT_HINT)}
                </p>
              </div>
            </div>

            <input
              type="text"
              value={supplementaryInput}
              onChange={(event) => setSupplementaryInput(event.target.value)}
              onClick={(event) => event.stopPropagation()}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault();
                  void handleReject();
                }
              }}
              placeholder={t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.INPUT_PLACEHOLDER)}
              disabled={interactiveDisabled}
              className="w-full rounded-md border theme-border theme-bg-main px-3 py-2 text-[12px] theme-text-primary placeholder:theme-text-secondary focus:border-[var(--accent-blue)] focus:outline-none focus:ring-1 focus:ring-[var(--accent-blue)] disabled:cursor-not-allowed disabled:opacity-60"
            />
          </div>
        </section>
        </div>
      </div>
    </div>
  );
}

function defaultScope(options: PermissionGrantOption[]): PermissionScopeType {
  if (options.some((option) => option.scopeType === PermissionScopeType.CONVERSATION)) {
    return PermissionScopeType.CONVERSATION;
  }
  return options[0]?.scopeType ?? PermissionScopeType.CONVERSATION;
}

function defaultCoverage(
  options: PermissionGrantOption[],
  scopeType: PermissionScopeType
): PermissionGrantCoverage {
  return permissionGrantCoverageOptionsForScope(options, scopeType)[0] ?? PermissionGrantCoverage.CONNECTION;
}

function permissionScopeHint(
  t: ReturnType<typeof useTranslation>['t'],
  scopeType: PermissionScopeType
): string {
  switch (scopeType) {
    case PermissionScopeType.CONVERSATION:
      return t(I18N_KEYS.AI.PERMISSION.SCOPE_HINT_CONVERSATION);
    case PermissionScopeType.USER:
      return t(I18N_KEYS.AI.PERMISSION.SCOPE_HINT_USER);
  }
  return '';
}

function permissionCoverageHint(
  t: ReturnType<typeof useTranslation>['t'],
  coverage: PermissionGrantCoverage,
  databaseName?: string | null,
  schemaName?: string | null
): string {
  switch (coverage) {
    case PermissionGrantCoverage.EXACT_TARGET:
      return t(I18N_KEYS.AI.PERMISSION.COVERAGE_HINT_EXACT_TARGET, {
        database: databaseName ?? '*',
        schema: schemaName ?? '*',
      });
    case PermissionGrantCoverage.DATABASE:
      return t(I18N_KEYS.AI.PERMISSION.COVERAGE_HINT_DATABASE, {
        database: databaseName ?? '*',
      });
    case PermissionGrantCoverage.CONNECTION:
      return t(I18N_KEYS.AI.PERMISSION.COVERAGE_HINT_CONNECTION);
  }
  return '';
}

function buildFollowupMessage(
  t: ReturnType<typeof useTranslation>['t'],
  option: PermissionGrantOption | null
): string {
  if (option == null) {
    return t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.RETRY_MESSAGE);
  }
  const scopeLabel = permissionScopeDescription(t, option.scopeType);
  const presetLabel = permissionGrantPresetLabel(t, option.grantPreset);
  return t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.RETRY_WITH_GRANT_MESSAGE, {
    scope: scopeLabel,
    preset: presetLabel,
  });
}

function buildRejectMessage(
  t: ReturnType<typeof useTranslation>['t'],
  supplementaryInput: string
): string {
  const trimmedInput = supplementaryInput.trim();
  if (!trimmedInput) {
    return t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.REJECT_MESSAGE);
  }
  return t(I18N_KEYS.AI.WRITE_EXECUTION_CONFIRM.CANCEL_WITH_INPUT_MESSAGE, { info: trimmedInput });
}

function normalizedCatalogName(option: PermissionGrantOption | null, databaseName?: string | null): string | null {
  if (option?.grantPreset === PermissionGrantPreset.CONNECTION_ALL_DATABASES) {
    return null;
  }
  return databaseName ?? null;
}

function normalizedSchemaName(option: PermissionGrantOption | null, schemaName?: string | null): string | null {
  if (option == null) {
    return schemaName ?? null;
  }
  if (option.grantPreset === PermissionGrantPreset.EXACT_SCHEMA) {
    return schemaName ?? null;
  }
  return null;
}
