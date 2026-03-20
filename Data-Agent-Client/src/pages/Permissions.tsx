import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { ArrowLeft, Pencil, Plus, RefreshCcw, ShieldCheck, Trash2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '../components/ui/Card';
import { Input } from '../components/ui/Input';
import { I18N_KEYS } from '../constants/i18nKeys';
import { ROUTES } from '../constants/routes';
import { useToast } from '../hooks/useToast';
import { resolveErrorMessage } from '../lib/errorMessage';
import {
  permissionCoverageToGrantPreset,
  permissionGrantPresetLabel,
  permissionGrantPresetToCoverage,
  permissionRuleLocation,
  permissionRuleSummary,
  permissionScopeLabel,
} from '../lib/permissionDisplay';
import { cn } from '../lib/utils';
import { connectionService } from '../services/connection.service';
import { permissionService } from '../services/permission.service';
import type { DbConnection } from '../types/connection';
import {
  PermissionGrantCoverage,
  PermissionScopeType,
  type PermissionRule,
  type PermissionUpsertRequest,
} from '../types/permission';

const FILTER_ALL = 'ALL';
const STATUS_FILTER = {
  ALL: 'ALL',
  ENABLED: 'ENABLED',
  DISABLED: 'DISABLED',
} as const;
const FORM_SELECT_CLASS_NAME = [
  'h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm',
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
  'disabled:cursor-not-allowed disabled:opacity-50',
].join(' ');

type PermissionStatusFilter = (typeof STATUS_FILTER)[keyof typeof STATUS_FILTER];
type PermissionFilterValue = typeof FILTER_ALL;
type PermissionEditorMode = 'create' | 'edit';

interface PermissionFiltersState {
  searchText: string;
  scopeType: PermissionScopeType | PermissionFilterValue;
  coverage: PermissionGrantCoverage | PermissionFilterValue;
  connectionId: string | PermissionFilterValue;
  status: PermissionStatusFilter;
}

interface PermissionFormState {
  scopeType: PermissionScopeType;
  connectionId: string;
  coverage: PermissionGrantCoverage;
  catalogName: string;
  schemaName: string;
  enabled: boolean;
}

interface PermissionFormErrors {
  scopeType?: string;
  connectionId?: string;
  catalogName?: string;
  schemaName?: string;
}

const defaultFilters: PermissionFiltersState = {
  searchText: '',
  scopeType: FILTER_ALL,
  coverage: FILTER_ALL,
  connectionId: FILTER_ALL,
  status: STATUS_FILTER.ALL,
};

function parseConversationId(value: string | null): number | null {
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

function createDefaultFormState(conversationId: number | null, preferConversation: boolean): PermissionFormState {
  return {
    scopeType: conversationId != null && preferConversation
      ? PermissionScopeType.CONVERSATION
      : PermissionScopeType.USER,
    connectionId: '',
    coverage: PermissionGrantCoverage.EXACT_TARGET,
    catalogName: '',
    schemaName: '',
    enabled: true,
  };
}

function mapRuleToFormState(rule: PermissionRule): PermissionFormState {
  return {
    scopeType: rule.scopeType,
    connectionId: String(rule.connectionId),
    coverage: permissionGrantPresetToCoverage(rule.grantPreset),
    catalogName: rule.catalogName ?? '',
    schemaName: rule.schemaName ?? '',
    enabled: rule.enabled,
  };
}

function buildPermissionPayload(
  form: PermissionFormState,
  conversationId: number | null,
  validationMessages: {
    connection: string;
    database: string;
    schema: string;
    context: string;
  },
): { payload?: PermissionUpsertRequest; errors: PermissionFormErrors } {
  const errors: PermissionFormErrors = {};
  const connectionId = Number(form.connectionId);
  const catalogName = normalizeOptionalText(form.catalogName);
  const schemaName = normalizeOptionalText(form.schemaName);

  if (!Number.isInteger(connectionId) || connectionId <= 0) {
    errors.connectionId = validationMessages.connection;
  }

  if (form.scopeType === PermissionScopeType.CONVERSATION && conversationId == null) {
    errors.scopeType = validationMessages.context;
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
      scopeType: form.scopeType,
      conversationId: form.scopeType === PermissionScopeType.CONVERSATION ? conversationId ?? undefined : undefined,
      connectionId,
      grantPreset: permissionCoverageToGrantPreset(form.coverage),
      catalogName: form.coverage === PermissionGrantCoverage.CONNECTION ? undefined : catalogName,
      schemaName: form.coverage === PermissionGrantCoverage.EXACT_TARGET ? schemaName : undefined,
      enabled: form.enabled,
    },
  };
}

function hasRuleDefinitionChanged(rule: PermissionRule, payload: PermissionUpsertRequest): boolean {
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

function formatCreatedAt(value?: string): string | null {
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

function PermissionStatCard({
  label,
  value,
}: {
  label: string;
  value: number;
}) {
  return (
    <Card className="border theme-border shadow-sm">
      <CardContent className="p-5">
        <div className="text-sm theme-text-secondary">{label}</div>
        <div className="mt-2 text-3xl font-semibold theme-text-primary">{value}</div>
      </CardContent>
    </Card>
  );
}

export default function Permissions() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const toast = useToast();
  const [searchParams] = useSearchParams();
  const requestedConversationId = useMemo(
    () => parseConversationId(searchParams.get('conversationId')),
    [searchParams],
  );

  const [showConversationContext, setShowConversationContext] = useState(requestedConversationId != null);
  const [rules, setRules] = useState<PermissionRule[]>([]);
  const [connections, setConnections] = useState<DbConnection[]>([]);
  const [filters, setFilters] = useState<PermissionFiltersState>(defaultFilters);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [toggleBusyId, setToggleBusyId] = useState<number | null>(null);
  const [deleteBusyId, setDeleteBusyId] = useState<number | null>(null);
  const [editorMode, setEditorMode] = useState<PermissionEditorMode>('create');
  const [editingRule, setEditingRule] = useState<PermissionRule | null>(null);
  const [form, setForm] = useState<PermissionFormState>(() => createDefaultFormState(requestedConversationId, true));
  const [formErrors, setFormErrors] = useState<PermissionFormErrors>({});

  useEffect(() => {
    setShowConversationContext(requestedConversationId != null);
    setEditorMode('create');
    setEditingRule(null);
    setForm(createDefaultFormState(requestedConversationId, requestedConversationId != null));
    setFormErrors({});
  }, [requestedConversationId]);

  const activeConversationId = showConversationContext ? requestedConversationId : null;

  const validationMessages = useMemo(() => ({
    connection: t(I18N_KEYS.PERMISSIONS_PAGE.VALIDATION_CONNECTION),
    database: t(I18N_KEYS.PERMISSIONS_PAGE.VALIDATION_DATABASE),
    schema: t(I18N_KEYS.PERMISSIONS_PAGE.VALIDATION_SCHEMA),
    context: t(I18N_KEYS.PERMISSIONS_PAGE.VALIDATION_CONTEXT),
  }), [t]);

  const reloadData = useCallback(async () => {
    setLoading(true);
    try {
      const [nextRules, nextConnections] = await Promise.all([
        permissionService.listRules(activeConversationId),
        connectionService.getConnections(),
      ]);
      setRules(nextRules);
      setConnections(nextConnections);
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.PERMISSIONS_PAGE.LOAD_FAILED)));
    } finally {
      setLoading(false);
    }
  }, [activeConversationId, t, toast]);

  useEffect(() => {
    void reloadData();
  }, [reloadData]);

  const connectionOptions = useMemo(() => {
    const optionMap = new Map<number, string>();
    for (const connection of connections) {
      optionMap.set(connection.id, connection.name);
    }
    for (const rule of rules) {
      if (!optionMap.has(rule.connectionId)) {
        optionMap.set(
          rule.connectionId,
          rule.connectionName || `${t(I18N_KEYS.PERMISSIONS_PAGE.UNKNOWN_CONNECTION)} #${rule.connectionId}`,
        );
      }
    }
    if (editingRule && !optionMap.has(editingRule.connectionId)) {
      optionMap.set(
        editingRule.connectionId,
        editingRule.connectionName || `${t(I18N_KEYS.PERMISSIONS_PAGE.UNKNOWN_CONNECTION)} #${editingRule.connectionId}`,
      );
    }
    return Array.from(optionMap.entries())
      .map(([id, name]) => ({ id, name }))
      .sort((left, right) => left.name.localeCompare(right.name));
  }, [connections, editingRule, rules, t]);

  const coverageOptions = useMemo(() => ([
    PermissionGrantCoverage.EXACT_TARGET,
    PermissionGrantCoverage.DATABASE,
    PermissionGrantCoverage.CONNECTION,
  ]), []);

  const filteredRules = useMemo(() => {
    const searchText = filters.searchText.trim().toLowerCase();

    return rules.filter((rule) => {
      if (filters.scopeType !== FILTER_ALL && rule.scopeType !== filters.scopeType) {
        return false;
      }

      if (
        filters.coverage !== FILTER_ALL
        && permissionGrantPresetToCoverage(rule.grantPreset) !== filters.coverage
      ) {
        return false;
      }

      if (filters.connectionId !== FILTER_ALL && String(rule.connectionId) !== filters.connectionId) {
        return false;
      }

      if (filters.status === STATUS_FILTER.ENABLED && !rule.enabled) {
        return false;
      }

      if (filters.status === STATUS_FILTER.DISABLED && rule.enabled) {
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
        permissionRuleSummary(t, rule),
        permissionRuleLocation(t, rule),
        permissionGrantPresetLabel(t, rule.grantPreset),
      ]
        .filter((value): value is string => typeof value === 'string' && value.trim() !== '')
        .join(' ')
        .toLowerCase();

      return haystack.includes(searchText);
    });
  }, [filters, rules, t]);

  const stats = useMemo(() => ({
    total: rules.length,
    enabled: rules.filter((rule) => rule.enabled).length,
    user: rules.filter((rule) => rule.scopeType === PermissionScopeType.USER).length,
    conversation: rules.filter((rule) => rule.scopeType === PermissionScopeType.CONVERSATION).length,
  }), [rules]);

  const resetEditor = useCallback((preferConversation: boolean) => {
    setEditorMode('create');
    setEditingRule(null);
    setForm(createDefaultFormState(requestedConversationId, preferConversation));
    setFormErrors({});
  }, [requestedConversationId]);

  const handleStartCreate = useCallback(() => {
    resetEditor(requestedConversationId != null && showConversationContext);
  }, [requestedConversationId, resetEditor, showConversationContext]);

  const handleEditRule = useCallback((rule: PermissionRule) => {
    setEditorMode('edit');
    setEditingRule(rule);
    setForm(mapRuleToFormState(rule));
    setFormErrors({});
  }, []);

  const handleFormChange = useCallback((
    field: keyof PermissionFormState,
    value: string | boolean,
  ) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setFormErrors((prev) => ({ ...prev, [field]: undefined }));
  }, []);

  const handleToggleRule = useCallback(async (rule: PermissionRule) => {
    if (toggleBusyId != null) {
      return;
    }

    setToggleBusyId(rule.id);
    try {
      await permissionService.setRuleEnabled(rule.id, !rule.enabled);
      setRules((prev) => prev.map((item) => (
        item.id === rule.id
          ? { ...item, enabled: !item.enabled }
          : item
      )));
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.PERMISSIONS_PAGE.TOGGLE_FAILED)));
    } finally {
      setToggleBusyId(null);
    }
  }, [t, toast, toggleBusyId]);

  const handleDeleteRule = useCallback(async (rule: PermissionRule) => {
    if (deleteBusyId != null) {
      return;
    }

    const confirmed = window.confirm(
      t(I18N_KEYS.PERMISSIONS_PAGE.DELETE_CONFIRM, {
        target: permissionRuleLocation(t, rule),
      }),
    );
    if (!confirmed) {
      return;
    }

    setDeleteBusyId(rule.id);
    try {
      await permissionService.deleteRule(rule.id);
      setRules((prev) => prev.filter((item) => item.id !== rule.id));
      if (editingRule?.id === rule.id) {
        resetEditor(requestedConversationId != null && showConversationContext);
      }
      toast.success(t(I18N_KEYS.PERMISSIONS_PAGE.DELETE_SUCCESS));
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.PERMISSIONS_PAGE.DELETE_FAILED)));
    } finally {
      setDeleteBusyId(null);
    }
  }, [deleteBusyId, editingRule?.id, requestedConversationId, resetEditor, showConversationContext, t, toast]);

  const handleSubmit = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const { payload, errors } = buildPermissionPayload(form, requestedConversationId, validationMessages);
    setFormErrors(errors);
    if (!payload) {
      return;
    }

    setSubmitting(true);
    try {
      const savedRule = await permissionService.upsertRule(payload);
      if (editingRule && hasRuleDefinitionChanged(editingRule, payload) && savedRule.id !== editingRule.id) {
        await permissionService.deleteRule(editingRule.id);
      }
      await reloadData();
      resetEditor(payload.scopeType === PermissionScopeType.CONVERSATION);
      toast.success(t(
        editorMode === 'edit'
          ? I18N_KEYS.PERMISSIONS_PAGE.UPDATE_SUCCESS
          : I18N_KEYS.PERMISSIONS_PAGE.CREATE_SUCCESS,
      ));
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.PERMISSIONS_PAGE.SAVE_FAILED)));
    } finally {
      setSubmitting(false);
    }
  }, [editingRule, editorMode, form, reloadData, requestedConversationId, resetEditor, t, toast, validationMessages]);

  const handleContextModeChange = useCallback((nextValue: boolean) => {
    setShowConversationContext(nextValue);
    resetEditor(nextValue);
  }, [resetEditor]);

  const coverageHint = useMemo(() => {
    if (form.coverage === PermissionGrantCoverage.EXACT_TARGET) {
      return t(I18N_KEYS.PERMISSIONS_PAGE.EXACT_HINT);
    }
    if (form.coverage === PermissionGrantCoverage.DATABASE) {
      return t(I18N_KEYS.PERMISSIONS_PAGE.DATABASE_HINT);
    }
    return t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_HINT);
  }, [form.coverage, t]);

  return (
    <div className="mx-auto flex max-w-7xl flex-col gap-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigate(ROUTES.HOME)}
              className="-ml-2 h-8 px-2 theme-text-secondary hover:theme-text-primary"
            >
              <ArrowLeft className="mr-1 h-4 w-4" />
              {t(I18N_KEYS.SETTINGS_PAGE.BACK_TO_WORKSPACE)}
            </Button>
          </div>
          <h1 className="text-3xl font-bold theme-text-primary">{t(I18N_KEYS.PERMISSIONS_PAGE.TITLE)}</h1>
          <p className="mt-2 max-w-3xl text-sm theme-text-secondary">
            {t(I18N_KEYS.PERMISSIONS_PAGE.SUBTITLE)}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void reloadData()}
            disabled={loading}
            className="gap-2"
          >
            <RefreshCcw className={cn('h-4 w-4', loading && 'animate-spin')} />
            {t(I18N_KEYS.COMMON.REFRESH)}
          </Button>
          <Button size="sm" onClick={handleStartCreate} className="gap-2">
            <Plus className="h-4 w-4" />
            {t(I18N_KEYS.PERMISSIONS_PAGE.CREATE_RULE)}
          </Button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <PermissionStatCard label={t(I18N_KEYS.PERMISSIONS_PAGE.STATS_TOTAL)} value={stats.total} />
        <PermissionStatCard label={t(I18N_KEYS.PERMISSIONS_PAGE.STATS_ENABLED)} value={stats.enabled} />
        <PermissionStatCard label={t(I18N_KEYS.PERMISSIONS_PAGE.STATS_USER)} value={stats.user} />
        <PermissionStatCard label={t(I18N_KEYS.PERMISSIONS_PAGE.STATS_CONVERSATION)} value={stats.conversation} />
      </div>

      <Card className="border theme-border shadow-sm">
        <CardContent className="flex flex-col gap-4 p-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-2 text-sm font-medium theme-text-primary">
              <ShieldCheck className="h-4 w-4 text-emerald-500" />
              {requestedConversationId != null && showConversationContext
                ? t(I18N_KEYS.PERMISSIONS_PAGE.CONTEXT_ACTIVE, { id: requestedConversationId })
                : t(I18N_KEYS.PERMISSIONS_PAGE.CONTEXT_INACTIVE)}
            </div>
            <p className="text-xs theme-text-secondary">
              {requestedConversationId == null
                ? t(I18N_KEYS.PERMISSIONS_PAGE.OPEN_FROM_CHAT_HINT)
                : t(I18N_KEYS.PERMISSIONS_PAGE.FILTERS_DESC)}
            </p>
          </div>
          {requestedConversationId != null && (
            <div className="flex items-center gap-2">
              <Button
                variant={showConversationContext ? 'default' : 'outline'}
                size="sm"
                onClick={() => handleContextModeChange(true)}
              >
                {t(I18N_KEYS.PERMISSIONS_PAGE.CONTEXT_ENABLE)}
              </Button>
              <Button
                variant={!showConversationContext ? 'default' : 'outline'}
                size="sm"
                onClick={() => handleContextModeChange(false)}
              >
                {t(I18N_KEYS.PERMISSIONS_PAGE.CONTEXT_DISABLE)}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-6 xl:grid-cols-[340px_minmax(0,1fr)]">
        <div className="space-y-6">
          <Card className="border theme-border shadow-sm xl:sticky xl:top-4">
            <CardHeader className="border-b theme-border">
              <CardTitle className="text-lg">{t(I18N_KEYS.PERMISSIONS_PAGE.FILTERS_TITLE)}</CardTitle>
              <CardDescription>{t(I18N_KEYS.PERMISSIONS_PAGE.FILTERS_DESC)}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4 p-5">
              <div className="space-y-2">
                <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-search">
                  {t(I18N_KEYS.COMMON.SEARCH)}
                </label>
                <Input
                  id="permission-search"
                  value={filters.searchText}
                  onChange={(event) => setFilters((prev) => ({ ...prev, searchText: event.target.value }))}
                  placeholder={t(I18N_KEYS.PERMISSIONS_PAGE.SEARCH_PLACEHOLDER)}
                />
              </div>

              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-1">
                <div className="space-y-2">
                  <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-filter-scope">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.SCOPE_LABEL)}
                  </label>
                  <select
                    id="permission-filter-scope"
                    className={FORM_SELECT_CLASS_NAME}
                    value={filters.scopeType}
                    onChange={(event) => setFilters((prev) => ({
                      ...prev,
                      scopeType: event.target.value as PermissionScopeType | PermissionFilterValue,
                    }))}
                  >
                    <option value={FILTER_ALL}>{t(I18N_KEYS.PERMISSIONS_PAGE.SCOPE_ALL)}</option>
                    <option value={PermissionScopeType.USER}>{permissionScopeLabel(t, PermissionScopeType.USER)}</option>
                    <option value={PermissionScopeType.CONVERSATION}>
                      {permissionScopeLabel(t, PermissionScopeType.CONVERSATION)}
                    </option>
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-filter-connection">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_LABEL)}
                  </label>
                  <select
                    id="permission-filter-connection"
                    className={FORM_SELECT_CLASS_NAME}
                    value={filters.connectionId}
                    onChange={(event) => setFilters((prev) => ({
                      ...prev,
                      connectionId: event.target.value,
                    }))}
                  >
                    <option value={FILTER_ALL}>{t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_ALL)}</option>
                    {connectionOptions.map((connection) => (
                      <option key={connection.id} value={String(connection.id)}>
                        {connection.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-filter-coverage">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.COVERAGE_LABEL)}
                  </label>
                  <select
                    id="permission-filter-coverage"
                    className={FORM_SELECT_CLASS_NAME}
                    value={filters.coverage}
                    onChange={(event) => setFilters((prev) => ({
                      ...prev,
                      coverage: event.target.value as PermissionGrantCoverage | PermissionFilterValue,
                    }))}
                  >
                    <option value={FILTER_ALL}>{t(I18N_KEYS.PERMISSIONS_PAGE.COVERAGE_ALL)}</option>
                    {coverageOptions.map((coverage) => (
                      <option key={coverage} value={coverage}>
                        {permissionGrantPresetLabel(t, permissionCoverageToGrantPreset(coverage))}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-filter-status">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.STATUS_LABEL)}
                  </label>
                  <select
                    id="permission-filter-status"
                    className={FORM_SELECT_CLASS_NAME}
                    value={filters.status}
                    onChange={(event) => setFilters((prev) => ({
                      ...prev,
                      status: event.target.value as PermissionStatusFilter,
                    }))}
                  >
                    <option value={STATUS_FILTER.ALL}>{t(I18N_KEYS.PERMISSIONS_PAGE.STATUS_ALL)}</option>
                    <option value={STATUS_FILTER.ENABLED}>{t(I18N_KEYS.PERMISSIONS_PAGE.STATUS_ENABLED)}</option>
                    <option value={STATUS_FILTER.DISABLED}>{t(I18N_KEYS.PERMISSIONS_PAGE.STATUS_DISABLED)}</option>
                  </select>
                </div>
              </div>

              <Button
                variant="ghost"
                size="sm"
                onClick={() => setFilters(defaultFilters)}
                className="w-full justify-center"
              >
                {t(I18N_KEYS.PERMISSIONS_PAGE.RESET_FILTERS)}
              </Button>
            </CardContent>
          </Card>

          <Card className="border theme-border shadow-sm xl:sticky xl:top-[420px]">
            <CardHeader className="border-b theme-border">
              <CardTitle className="text-lg">
                {editorMode === 'edit'
                  ? t(I18N_KEYS.PERMISSIONS_PAGE.EDIT_TITLE)
                  : t(I18N_KEYS.PERMISSIONS_PAGE.CREATE_TITLE)}
              </CardTitle>
              <CardDescription>
                {editorMode === 'edit'
                  ? t(I18N_KEYS.PERMISSIONS_PAGE.EDIT_DESC)
                  : t(I18N_KEYS.PERMISSIONS_PAGE.CREATE_DESC)}
              </CardDescription>
            </CardHeader>
            <CardContent className="p-5">
              <form className="space-y-4" onSubmit={handleSubmit}>
                <div className="space-y-2">
                  <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-form-scope">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.SCOPE_LABEL)}
                  </label>
                  <select
                    id="permission-form-scope"
                    className={cn(
                      FORM_SELECT_CLASS_NAME,
                      formErrors.scopeType && 'border-red-500 focus-visible:ring-red-500',
                    )}
                    value={form.scopeType}
                    onChange={(event) => handleFormChange('scopeType', event.target.value as PermissionScopeType)}
                  >
                    <option value={PermissionScopeType.USER}>{permissionScopeLabel(t, PermissionScopeType.USER)}</option>
                    <option value={PermissionScopeType.CONVERSATION} disabled={requestedConversationId == null}>
                      {permissionScopeLabel(t, PermissionScopeType.CONVERSATION)}
                    </option>
                  </select>
                  {(formErrors.scopeType || requestedConversationId == null) && (
                    <p className={cn('text-xs', formErrors.scopeType ? 'text-red-500' : 'theme-text-secondary')}>
                      {formErrors.scopeType || t(I18N_KEYS.PERMISSIONS_PAGE.OPEN_FROM_CHAT_HINT)}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-form-connection">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_LABEL)}
                  </label>
                  <select
                    id="permission-form-connection"
                    className={cn(
                      FORM_SELECT_CLASS_NAME,
                      formErrors.connectionId && 'border-red-500 focus-visible:ring-red-500',
                    )}
                    value={form.connectionId}
                    onChange={(event) => handleFormChange('connectionId', event.target.value)}
                  >
                    <option value="">
                      {connectionOptions.length > 0
                        ? t(I18N_KEYS.PERMISSIONS_PAGE.CONNECTION_LABEL)
                        : t(I18N_KEYS.COMMON.NO_CONNECTIONS)}
                    </option>
                    {connectionOptions.map((connection) => (
                      <option key={connection.id} value={String(connection.id)}>
                        {connection.name}
                      </option>
                    ))}
                  </select>
                  {formErrors.connectionId && (
                    <p className="text-xs text-red-500">{formErrors.connectionId}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-form-coverage">
                    {t(I18N_KEYS.PERMISSIONS_PAGE.COVERAGE_LABEL)}
                  </label>
                  <select
                    id="permission-form-coverage"
                    className={FORM_SELECT_CLASS_NAME}
                    value={form.coverage}
                    onChange={(event) => handleFormChange('coverage', event.target.value as PermissionGrantCoverage)}
                  >
                    {coverageOptions.map((coverage) => (
                      <option key={coverage} value={coverage}>
                        {permissionGrantPresetLabel(t, permissionCoverageToGrantPreset(coverage))}
                      </option>
                    ))}
                  </select>
                  <p className="text-xs theme-text-secondary">{coverageHint}</p>
                </div>

                {form.coverage !== PermissionGrantCoverage.CONNECTION && (
                  <div className="space-y-2">
                    <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-form-database">
                      {t(I18N_KEYS.PERMISSIONS_PAGE.DATABASE_LABEL)}
                    </label>
                    <Input
                      id="permission-form-database"
                      value={form.catalogName}
                      onChange={(event: ChangeEvent<HTMLInputElement>) => handleFormChange('catalogName', event.target.value)}
                      placeholder={t(I18N_KEYS.PERMISSIONS_PAGE.DATABASE_PLACEHOLDER)}
                      className={cn(formErrors.catalogName && 'border-red-500 focus-visible:ring-red-500')}
                    />
                    {formErrors.catalogName && (
                      <p className="text-xs text-red-500">{formErrors.catalogName}</p>
                    )}
                  </div>
                )}

                {form.coverage === PermissionGrantCoverage.EXACT_TARGET && (
                  <div className="space-y-2">
                    <label className="text-xs font-medium theme-text-secondary" htmlFor="permission-form-schema">
                      {t(I18N_KEYS.PERMISSIONS_PAGE.SCHEMA_LABEL)}
                    </label>
                    <Input
                      id="permission-form-schema"
                      value={form.schemaName}
                      onChange={(event: ChangeEvent<HTMLInputElement>) => handleFormChange('schemaName', event.target.value)}
                      placeholder={t(I18N_KEYS.PERMISSIONS_PAGE.SCHEMA_PLACEHOLDER)}
                      className={cn(formErrors.schemaName && 'border-red-500 focus-visible:ring-red-500')}
                    />
                    {formErrors.schemaName && (
                      <p className="text-xs text-red-500">{formErrors.schemaName}</p>
                    )}
                  </div>
                )}

                <label className="flex items-center gap-3 rounded-md border theme-border px-3 py-2 text-sm">
                  <input
                    type="checkbox"
                    checked={form.enabled}
                    onChange={(event) => handleFormChange('enabled', event.target.checked)}
                    className="h-4 w-4 rounded border-input text-primary focus:ring-ring"
                  />
                  <span className="theme-text-primary">{t(I18N_KEYS.PERMISSIONS_PAGE.ENABLED_LABEL)}</span>
                </label>

                <div className="flex flex-col gap-2 sm:flex-row">
                  <Button
                    type="submit"
                    disabled={submitting || connectionOptions.length === 0}
                    className="sm:flex-1"
                  >
                    {editorMode === 'edit'
                      ? t(I18N_KEYS.PERMISSIONS_PAGE.SAVE_UPDATE)
                      : t(I18N_KEYS.PERMISSIONS_PAGE.SAVE_CREATE)}
                  </Button>
                  {editorMode === 'edit' && (
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => resetEditor(requestedConversationId != null && showConversationContext)}
                      className="sm:flex-1"
                    >
                      {t(I18N_KEYS.PERMISSIONS_PAGE.CANCEL_EDIT)}
                    </Button>
                  )}
                </div>
              </form>
            </CardContent>
          </Card>
        </div>

        <Card className="border theme-border shadow-sm">
          <CardHeader className="border-b theme-border">
            <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
              <div>
                <CardTitle className="text-lg">{t(I18N_KEYS.PERMISSIONS_PAGE.LIST_TITLE)}</CardTitle>
                <CardDescription>{t(I18N_KEYS.PERMISSIONS_PAGE.LIST_DESC)}</CardDescription>
              </div>
              <div className="rounded-full border theme-border px-3 py-1 text-xs theme-text-secondary">
                {t(I18N_KEYS.PERMISSIONS_PAGE.LIST_COUNT, { count: filteredRules.length })}
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4 p-5">
            {loading ? (
              <div className="rounded-lg border border-dashed theme-border px-4 py-12 text-center text-sm theme-text-secondary">
                {t(I18N_KEYS.AI.PERMISSION.LOADING)}
              </div>
            ) : filteredRules.length === 0 ? (
              <div className="rounded-lg border border-dashed theme-border px-4 py-12 text-center">
                <div className="text-sm font-medium theme-text-primary">
                  {t(I18N_KEYS.PERMISSIONS_PAGE.EMPTY_TITLE)}
                </div>
                <p className="mt-2 text-sm theme-text-secondary">
                  {t(I18N_KEYS.PERMISSIONS_PAGE.EMPTY_DESC)}
                </p>
              </div>
            ) : (
              filteredRules.map((rule) => {
                const createdAt = formatCreatedAt(rule.createdAt);
                const isSelected = editingRule?.id === rule.id;
                const isToggleBusy = toggleBusyId === rule.id;
                const isDeleteBusy = deleteBusyId === rule.id;

                return (
                  <div
                    key={rule.id}
                    className={cn(
                      'rounded-xl border p-4 transition-colors',
                      isSelected ? 'border-blue-500 bg-blue-500/5' : 'theme-border theme-bg-panel',
                    )}
                  >
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div className="space-y-3">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-sm font-semibold theme-text-primary">
                            {rule.connectionName || `${t(I18N_KEYS.PERMISSIONS_PAGE.UNKNOWN_CONNECTION)} #${rule.connectionId}`}
                          </span>
                          <span className="rounded-full border theme-border px-2 py-0.5 text-[11px] theme-text-secondary">
                            {permissionScopeLabel(t, rule.scopeType)}
                          </span>
                          <span
                            className={cn(
                              'rounded-full px-2 py-0.5 text-[11px] font-medium',
                              rule.enabled
                                ? 'bg-emerald-500/10 text-emerald-600'
                                : 'bg-slate-500/10 theme-text-secondary',
                            )}
                          >
                            {rule.enabled ? t(I18N_KEYS.AI.PERMISSION.ENABLED) : t(I18N_KEYS.AI.PERMISSION.DISABLED)}
                          </span>
                        </div>

                        <div className="text-sm leading-6 theme-text-primary">
                          {permissionRuleSummary(t, rule)}
                        </div>

                        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs theme-text-secondary">
                          <span>{permissionGrantPresetLabel(t, rule.grantPreset)}</span>
                          <span>{permissionRuleLocation(t, rule)}</span>
                          {createdAt && (
                            <span>{t(I18N_KEYS.PERMISSIONS_PAGE.CREATED_AT, { value: createdAt })}</span>
                          )}
                        </div>
                      </div>

                      <div className="flex flex-wrap items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleEditRule(rule)}
                          className="gap-2"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                          {t(I18N_KEYS.PERMISSIONS_PAGE.EDIT)}
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => void handleToggleRule(rule)}
                          disabled={isToggleBusy}
                        >
                          {rule.enabled
                            ? t(I18N_KEYS.PERMISSIONS_PAGE.DISABLE)
                            : t(I18N_KEYS.PERMISSIONS_PAGE.ENABLE)}
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => void handleDeleteRule(rule)}
                          disabled={isDeleteBusy}
                          className="gap-2"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                          {t(I18N_KEYS.PERMISSIONS_PAGE.DELETE)}
                        </Button>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
