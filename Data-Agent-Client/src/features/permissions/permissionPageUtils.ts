import { permissionCoverageToGrantPreset, permissionGrantPresetToCoverage } from '../../lib/permissionDisplay';
import {
  PermissionGrantCoverage,
  PermissionScopeType,
  type PermissionRule,
  type PermissionUpsertRequest,
} from '../../types/permission';
import {
  PERMISSION_FILTER_VALUE,
  PERMISSION_STATUS_FILTER,
} from './permissionPageConstants';
import type {
  PermissionConnectionOption,
  PermissionConversationOption,
  PermissionFilterFormState,
  PermissionFormErrors,
  PermissionFormState,
} from './permissionPageModels';

export const defaultPermissionFilterFormState: PermissionFilterFormState = {
  searchText: '',
  coverage: PERMISSION_FILTER_VALUE.ALL,
  connectionId: PERMISSION_FILTER_VALUE.ALL,
  status: PERMISSION_STATUS_FILTER.ALL,
};

export function parseConversationId(value: string | null): number | null {
  if (value == null || value.trim() === '') {
    return null;
  }

  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

function normalizeOptionalText(value: string): string | undefined {
  const nextValue = value.trim();
  return nextValue === '' ? undefined : nextValue;
}

export function createDefaultPermissionFormState(): PermissionFormState {
  return {
    connectionId: '',
    coverage: PermissionGrantCoverage.EXACT_TARGET,
    catalogName: '',
    schemaName: '',
    enabled: true,
  };
}

export function mapPermissionRuleToFormState(rule: PermissionRule): PermissionFormState {
  return {
    connectionId: String(rule.connectionId),
    coverage: permissionGrantPresetToCoverage(rule.grantPreset),
    catalogName: rule.catalogName ?? '',
    schemaName: rule.schemaName ?? '',
    enabled: rule.enabled,
  };
}

export function buildPermissionPayload(
  form: PermissionFormState,
  scopeType: PermissionScopeType,
  conversationId: number | null,
  validationMessages: {
    conversation: string;
    connection: string;
    database: string;
    schema: string;
  },
): { payload?: PermissionUpsertRequest; errors: PermissionFormErrors } {
  const errors: PermissionFormErrors = {};
  const connectionId = Number(form.connectionId);
  const catalogName = normalizeOptionalText(form.catalogName);
  const schemaName = normalizeOptionalText(form.schemaName);

  if (scopeType === PermissionScopeType.CONVERSATION && conversationId == null) {
    errors.conversationId = validationMessages.conversation;
  }

  if (!Number.isInteger(connectionId) || connectionId <= 0) {
    errors.connectionId = validationMessages.connection;
  }

  if (
    (form.coverage === PermissionGrantCoverage.EXACT_TARGET || form.coverage === PermissionGrantCoverage.DATABASE)
    && !catalogName
  ) {
    errors.catalogName = validationMessages.database;
  }

  if (form.coverage === PermissionGrantCoverage.EXACT_TARGET && !schemaName) {
    errors.schemaName = validationMessages.schema;
  }

  if (Object.keys(errors).length > 0) {
    return { errors };
  }

  return {
    errors,
    payload: {
      scopeType,
      conversationId: scopeType === PermissionScopeType.CONVERSATION ? conversationId ?? undefined : undefined,
      connectionId,
      grantPreset: permissionCoverageToGrantPreset(form.coverage),
      catalogName: form.coverage === PermissionGrantCoverage.CONNECTION ? undefined : catalogName,
      schemaName: form.coverage === PermissionGrantCoverage.EXACT_TARGET ? schemaName : undefined,
      enabled: form.enabled,
    },
  };
}

export function hasRuleDefinitionChanged(rule: PermissionRule, payload: PermissionUpsertRequest): boolean {
  const payloadConversationId = payload.scopeType === PermissionScopeType.CONVERSATION
    ? payload.conversationId ?? null
    : null;
  const ruleConversationId = rule.scopeType === PermissionScopeType.CONVERSATION
    ? rule.conversationId ?? null
    : null;

  return (
    rule.scopeType !== payload.scopeType
    || ruleConversationId !== payloadConversationId
    || rule.connectionId !== payload.connectionId
    || rule.grantPreset !== payload.grantPreset
    || (rule.catalogName ?? null) !== (payload.catalogName ?? null)
    || (rule.schemaName ?? null) !== (payload.schemaName ?? null)
  );
}

export function formatPermissionCreatedAt(value?: string): string | null {
  if (!value) {
    return null;
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return null;
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(parsed);
}

export function buildPermissionConnectionOptions(
  connections: Array<{ id: number; name: string }>,
  rules: PermissionRule[],
  editingRule: PermissionRule | null,
  unknownConnectionLabel: string,
): PermissionConnectionOption[] {
  const optionMap = new Map<number, string>();
  for (const connection of connections) {
    optionMap.set(connection.id, connection.name);
  }
  for (const rule of rules) {
    if (!optionMap.has(rule.connectionId)) {
      optionMap.set(
        rule.connectionId,
        rule.connectionName || `${unknownConnectionLabel} #${rule.connectionId}`,
      );
    }
  }
  if (editingRule && !optionMap.has(editingRule.connectionId)) {
    optionMap.set(
      editingRule.connectionId,
      editingRule.connectionName || `${unknownConnectionLabel} #${editingRule.connectionId}`,
    );
  }
  return Array.from(optionMap.entries())
    .map(([id, name]) => ({ id, name }))
    .sort((left, right) => left.name.localeCompare(right.name));
}

export function buildPermissionConversationOptions(
  conversations: Array<{ id: number; title: string | null }>,
  editingConversationId: number | null | undefined,
  requestedConversationId: number | null,
): PermissionConversationOption[] {
  const optionMap = new Map<number, string>();
  for (const conversation of conversations) {
    optionMap.set(
      conversation.id,
      conversation.title?.trim() ? `${conversation.title} (#${conversation.id})` : `#${conversation.id}`,
    );
  }
  if (editingConversationId != null && !optionMap.has(editingConversationId)) {
    optionMap.set(editingConversationId, `#${editingConversationId}`);
  }
  if (requestedConversationId != null && !optionMap.has(requestedConversationId)) {
    optionMap.set(requestedConversationId, `#${requestedConversationId}`);
  }
  return Array.from(optionMap.entries())
    .map(([id, label]) => ({ id, label }))
    .sort((left, right) => right.id - left.id);
}

export function filterPermissionRules(
  rules: PermissionRule[],
  selectedScopeType: PermissionScopeType,
  filters: PermissionFilterFormState,
): PermissionRule[] {
  const searchText = filters.searchText.trim().toLowerCase();

  return rules.filter((rule) => {
    if (rule.scopeType !== selectedScopeType) {
      return false;
    }

    if (
      filters.coverage !== PERMISSION_FILTER_VALUE.ALL
      && permissionGrantPresetToCoverage(rule.grantPreset) !== filters.coverage
    ) {
      return false;
    }

    if (
      filters.connectionId !== PERMISSION_FILTER_VALUE.ALL
      && String(rule.connectionId) !== filters.connectionId
    ) {
      return false;
    }

    if (filters.status === PERMISSION_STATUS_FILTER.ENABLED && !rule.enabled) {
      return false;
    }

    if (filters.status === PERMISSION_STATUS_FILTER.DISABLED && rule.enabled) {
      return false;
    }

    if (searchText === '') {
      return true;
    }

    const haystack = [
      rule.connectionName,
      String(rule.connectionId),
      rule.catalogName,
      rule.schemaName,
      rule.grantPreset,
    ]
      .filter((value): value is string => typeof value === 'string' && value.trim() !== '')
      .join(' ')
      .toLowerCase();

    return haystack.includes(searchText);
  });
}
