import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ChangeEvent, FormEvent, SetStateAction } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useToast } from '../../hooks/useToast';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { connectionService } from '../../services/connection.service';
import { permissionService } from '../../services/permission.service';
import type { DbConnection } from '../../types/connection';
import { PermissionGrantCoverage, PermissionScopeType, type PermissionRule } from '../../types/permission';
import { PERMISSION_COVERAGE_OPTIONS } from './permissionPageConstants';
import type {
  PermissionFilterFormState,
} from './permissionPageModels';
import {
  defaultPermissionFilterFormState,
  filterPermissionRules,
} from './permissionPageUtils';

interface UsePermissionListStateReturn {
  rules: PermissionRule[];
  connections: DbConnection[];
  coverageOptions: PermissionGrantCoverage[];
  filterForm: PermissionFilterFormState;
  filteredRules: PermissionRule[];
  listLoading: boolean;
  setRules: (updater: SetStateAction<PermissionRule[]>) => void;
  loadRules: () => Promise<void>;
  handleFilterInputChange: (
    field: keyof PermissionFilterFormState,
  ) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  handleApplyFilters: (event: FormEvent) => void;
  handleResetFilters: () => void;
}

export function usePermissionListState(
  requestedConversationId: number | null,
  selectedScopeType: PermissionScopeType,
): UsePermissionListStateReturn {
  const { t } = useTranslation();
  const toast = useToast();
  const [rules, setRules] = useState<PermissionRule[]>([]);
  const [connections, setConnections] = useState<DbConnection[]>([]);
  const [filterForm, setFilterForm] = useState<PermissionFilterFormState>(defaultPermissionFilterFormState);
  const [filters, setFilters] = useState<PermissionFilterFormState>(defaultPermissionFilterFormState);
  const [listLoading, setListLoading] = useState(false);

  const coverageOptions = useMemo(() => [...PERMISSION_COVERAGE_OPTIONS], []);

  const loadRules = useCallback(async () => {
    setListLoading(true);
    try {
      const [nextRules, nextConnections] = await Promise.all([
        permissionService.listRules({
          scopeType: selectedScopeType,
          conversationId: selectedScopeType === PermissionScopeType.CONVERSATION ? requestedConversationId : undefined,
        }),
        connectionService.getConnections(),
      ]);
      setRules(nextRules);
      setConnections(nextConnections);
    } catch (error) {
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.PERMISSIONS_PAGE.LOAD_FAILED)));
    } finally {
      setListLoading(false);
    }
  }, [requestedConversationId, selectedScopeType, t, toast]);

  useEffect(() => {
    void loadRules();
  }, [loadRules]);

  const filteredRules = useMemo(
    () => filterPermissionRules(rules, selectedScopeType, filters),
    [filters, rules, selectedScopeType],
  );

  const handleFilterInputChange =
    (field: keyof PermissionFilterFormState) =>
    (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      setFilterForm((prev) => ({ ...prev, [field]: event.target.value }));
    };

  const handleApplyFilters = (event: FormEvent) => {
    event.preventDefault();
    setFilters(filterForm);
  };

  const handleResetFilters = () => {
    setFilterForm(defaultPermissionFilterFormState);
    setFilters(defaultPermissionFilterFormState);
  };

  return {
    rules,
    connections,
    coverageOptions,
    filterForm,
    filteredRules,
    listLoading,
    setRules,
    loadRules,
    handleFilterInputChange,
    handleApplyFilters,
    handleResetFilters,
  };
}
