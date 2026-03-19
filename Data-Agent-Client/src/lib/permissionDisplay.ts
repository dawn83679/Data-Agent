import type { TFunction } from 'i18next';
import { I18N_KEYS } from '../constants/i18nKeys';
import {
  PermissionGrantCoverage,
  PermissionGrantPreset,
  PermissionScopeType,
  type PermissionGrantOption,
  type PermissionRule,
} from '../types/permission';

const COVERAGE_ORDER: PermissionGrantCoverage[] = [
  PermissionGrantCoverage.EXACT_TARGET,
  PermissionGrantCoverage.DATABASE,
  PermissionGrantCoverage.CONNECTION,
];

export function permissionScopeLabel(t: TFunction, scopeType: PermissionScopeType): string {
  switch (scopeType) {
    case PermissionScopeType.CONVERSATION:
      return t(I18N_KEYS.AI.PERMISSION.SCOPE_CONVERSATION);
    case PermissionScopeType.USER:
      return t(I18N_KEYS.AI.PERMISSION.SCOPE_USER);
  }
  return '';
}

export function permissionScopeDescription(t: TFunction, scopeType: PermissionScopeType): string {
  switch (scopeType) {
    case PermissionScopeType.CONVERSATION:
      return t(I18N_KEYS.AI.PERMISSION.SCOPE_DESC_CONVERSATION);
    case PermissionScopeType.USER:
      return t(I18N_KEYS.AI.PERMISSION.SCOPE_DESC_USER);
  }
  return '';
}

export function permissionGrantPresetLabel(t: TFunction, grantPreset: PermissionGrantPreset): string {
  switch (grantPreset) {
    case PermissionGrantPreset.EXACT_SCHEMA:
      return t(I18N_KEYS.AI.PERMISSION.PRESET_EXACT_SCHEMA);
    case PermissionGrantPreset.DATABASE_ALL_SCHEMAS:
      return t(I18N_KEYS.AI.PERMISSION.PRESET_DATABASE_ALL_SCHEMAS);
    case PermissionGrantPreset.CONNECTION_ALL_DATABASES:
      return t(I18N_KEYS.AI.PERMISSION.PRESET_CONNECTION_ALL_DATABASES);
  }
  return '';
}

export function permissionGrantPresetToCoverage(grantPreset: PermissionGrantPreset): PermissionGrantCoverage {
  switch (grantPreset) {
    case PermissionGrantPreset.EXACT_SCHEMA:
      return PermissionGrantCoverage.EXACT_TARGET;
    case PermissionGrantPreset.DATABASE_ALL_SCHEMAS:
      return PermissionGrantCoverage.DATABASE;
    case PermissionGrantPreset.CONNECTION_ALL_DATABASES:
      return PermissionGrantCoverage.CONNECTION;
  }
}

export function permissionCoverageLabel(
  t: TFunction,
  coverage: PermissionGrantCoverage,
  catalogName?: string | null,
  schemaName?: string | null
): string {
  switch (coverage) {
    case PermissionGrantCoverage.EXACT_TARGET:
      return t(I18N_KEYS.AI.PERMISSION.COVERAGE_EXACT_TARGET, {
        database: catalogName ?? '*',
        schema: schemaName ?? '*',
      });
    case PermissionGrantCoverage.DATABASE:
      return t(I18N_KEYS.AI.PERMISSION.COVERAGE_DATABASE, {
        database: catalogName ?? '*',
      });
    case PermissionGrantCoverage.CONNECTION:
      return t(I18N_KEYS.AI.PERMISSION.COVERAGE_CONNECTION);
  }
  return '';
}

export function permissionTargetLabel(
  t: TFunction,
  grantPreset: PermissionGrantPreset,
  catalogName?: string | null,
  schemaName?: string | null
): string {
  return permissionCoverageLabel(
    t,
    permissionGrantPresetToCoverage(grantPreset),
    catalogName,
    schemaName
  );
}

export function permissionGrantCoverageOptions(
  options: PermissionGrantOption[]
): PermissionGrantCoverage[] {
  const seen = new Set<PermissionGrantCoverage>();
  for (const option of options) {
    seen.add(permissionGrantPresetToCoverage(option.grantPreset));
  }
  return COVERAGE_ORDER.filter((coverage) => seen.has(coverage));
}

export function permissionScopeOptions(options: PermissionGrantOption[]): PermissionScopeType[] {
  return Array.from(new Set(options.map((option) => option.scopeType)));
}

export function permissionGrantCoverageOptionsForScope(
  options: PermissionGrantOption[],
  scopeType: PermissionScopeType
): PermissionGrantCoverage[] {
  return permissionGrantCoverageOptions(
    options.filter((option) => option.scopeType === scopeType)
  );
}

export function permissionFindGrantOption(
  options: PermissionGrantOption[],
  scopeType: PermissionScopeType,
  coverage: PermissionGrantCoverage
): PermissionGrantOption | null {
  const grantPreset = permissionCoverageToGrantPreset(coverage);
  return options.find((option) => option.scopeType === scopeType && option.grantPreset === grantPreset) ?? null;
}

export function permissionCycleScope(
  options: PermissionGrantOption[],
  currentScope: PermissionScopeType
): PermissionScopeType {
  const scopes = permissionScopeOptions(options);
  if (scopes.length <= 1) {
    return currentScope;
  }
  const currentIndex = scopes.indexOf(currentScope);
  if (currentIndex < 0) {
    return scopes[0] ?? currentScope;
  }
  return scopes[(currentIndex + 1) % scopes.length] ?? currentScope;
}

export function permissionCycleCoverage(
  options: PermissionGrantOption[],
  currentCoverage: PermissionGrantCoverage,
  scopeType?: PermissionScopeType
): PermissionGrantCoverage {
  const coverages = scopeType == null
    ? permissionGrantCoverageOptions(options)
    : permissionGrantCoverageOptionsForScope(options, scopeType);
  if (coverages.length <= 1) {
    return currentCoverage;
  }
  const currentIndex = coverages.indexOf(currentCoverage);
  if (currentIndex < 0) {
    return coverages[0] ?? currentCoverage;
  }
  return coverages[(currentIndex + 1) % coverages.length] ?? currentCoverage;
}

export function permissionCoverageToGrantPreset(coverage: PermissionGrantCoverage): PermissionGrantPreset {
  switch (coverage) {
    case PermissionGrantCoverage.EXACT_TARGET:
      return PermissionGrantPreset.EXACT_SCHEMA;
    case PermissionGrantCoverage.DATABASE:
      return PermissionGrantPreset.DATABASE_ALL_SCHEMAS;
    case PermissionGrantCoverage.CONNECTION:
      return PermissionGrantPreset.CONNECTION_ALL_DATABASES;
  }
}

export function permissionDefaultAllowSentence(
  t: TFunction,
  scopeType: PermissionScopeType,
  coverage: PermissionGrantCoverage,
  catalogName?: string | null,
  schemaName?: string | null
): string {
  return t(I18N_KEYS.AI.PERMISSION.SUMMARY_ALLOW, {
    scope: permissionScopeDescription(t, scopeType),
    target: permissionCoverageLabel(t, coverage, catalogName, schemaName),
  });
}

export function permissionRuleSummary(t: TFunction, rule: PermissionRule): string {
  return permissionDefaultAllowSentence(
    t,
    rule.scopeType,
    permissionGrantPresetToCoverage(rule.grantPreset),
    rule.catalogName,
    rule.schemaName
  );
}

export function permissionRuleLocation(t: TFunction, rule: PermissionRule): string {
  return permissionTargetLabel(t, rule.grantPreset, rule.catalogName, rule.schemaName);
}
