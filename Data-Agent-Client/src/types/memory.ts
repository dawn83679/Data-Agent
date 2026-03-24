import type { PageResponse } from './conversation';

export const MEMORY_TYPE_OPTIONS = [
  'PREFERENCE',
  'BUSINESS_RULE',
  'KNOWLEDGE_POINT',
  'GOLDEN_SQL_CASE',
  'WORKFLOW_CONSTRAINT',
] as const;

export type KnownMemoryType = (typeof MEMORY_TYPE_OPTIONS)[number];
export type MemoryType = KnownMemoryType | (string & {});

export const MEMORY_SUBTYPE_OPTIONS_BY_TYPE = {
  PREFERENCE: ['RESPONSE_FORMAT', 'LANGUAGE_PREFERENCE'],
  BUSINESS_RULE: ['PRODUCT_RULE', 'DOMAIN_RULE', 'GOVERNANCE_RULE', 'SAFETY_RULE'],
  KNOWLEDGE_POINT: ['ARCHITECTURE_KNOWLEDGE', 'DOMAIN_KNOWLEDGE', 'GLOSSARY', 'OBJECT_KNOWLEDGE'],
  WORKFLOW_CONSTRAINT: ['PROCESS_RULE', 'APPROVAL_RULE', 'IMPLEMENTATION_CONSTRAINT', 'REVIEW_CONSTRAINT'],
  GOLDEN_SQL_CASE: ['QUERY_PATTERN', 'JOIN_STRATEGY', 'VALIDATED_SQL', 'METRIC_CALCULATION'],
} as const satisfies Record<KnownMemoryType, readonly string[]>;

export type KnownMemorySubType = (typeof MEMORY_SUBTYPE_OPTIONS_BY_TYPE)[KnownMemoryType][number];
export type MemorySubType = KnownMemorySubType | (string & {});

export const MEMORY_SCOPE_OPTIONS = ['USER', 'CONVERSATION'] as const;
export type KnownMemoryScope = (typeof MEMORY_SCOPE_OPTIONS)[number];
export type MemoryScope = KnownMemoryScope | (string & {});

export const MEMORY_MANUAL_SCOPE_OPTIONS = ['USER', 'CONVERSATION'] as const;

export const MEMORY_SOURCE_TYPE_OPTIONS = ['MANUAL', 'AGENT'] as const;
export type KnownMemorySourceType = (typeof MEMORY_SOURCE_TYPE_OPTIONS)[number];
export type MemorySourceType = KnownMemorySourceType | (string & {});

export const MEMORY_ENABLE = {
  DISABLE: 0,
  ENABLE: 1,
} as const;

export type MemoryEnable = (typeof MEMORY_ENABLE)[keyof typeof MEMORY_ENABLE];

export const isKnownMemoryType = (value?: string | null): value is KnownMemoryType =>
  !!value && MEMORY_TYPE_OPTIONS.includes(value as KnownMemoryType);

export const isMemoryScope = (value?: string | null): value is KnownMemoryScope =>
  !!value && MEMORY_SCOPE_OPTIONS.includes(value as KnownMemoryScope);

export const isMemorySourceType = (value?: string | null): value is KnownMemorySourceType =>
  !!value && MEMORY_SOURCE_TYPE_OPTIONS.includes(value as KnownMemorySourceType);

export const getMemorySubtypeOptions = (memoryType?: string | null): readonly MemorySubType[] => {
  if (!isKnownMemoryType(memoryType)) {
    return [];
  }
  return MEMORY_SUBTYPE_OPTIONS_BY_TYPE[memoryType];
};

export const isMemorySubtypeForType = (
  memoryType?: string | null,
  subType?: string | null,
): subType is MemorySubType => {
  if (!subType) {
    return false;
  }
  return getMemorySubtypeOptions(memoryType).includes(subType as MemorySubType);
};

export const getDefaultMemorySubtype = (memoryType?: string | null): MemorySubType | '' => {
  const [firstSubType] = getMemorySubtypeOptions(memoryType);
  return firstSubType ?? '';
};

export interface Memory {
  id: number;
  conversationId?: number | null;
  scope: MemoryScope;
  memoryType: MemoryType;
  subType?: MemorySubType | null;
  sourceType: MemorySourceType;
  title: string;
  content: string;
  reason?: string | null;
  enable: MemoryEnable;
  accessCount?: number | null;
  lastAccessedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MemoryListParams {
  current?: number;
  size?: number;
  keyword?: string;
  memoryType?: MemoryType;
  enable?: number;
  scope?: MemoryScope;
}

export interface MemorySearchRequest {
  queryText: string;
  limit?: number;
  minScore?: number;
  memoryType?: MemoryType;
  scope?: MemoryScope;
}

export interface MemorySearchResult {
  id: number;
  memoryType: MemoryType;
  content: string;
  score: number;
  conversationId?: number | null;
}

export interface MemoryMaintenanceReport {
  generatedAt: string;
  enabledMemoryCount: number;
  disabledMemoryCount: number;
  duplicateEnabledMemoryCount: number;
  processedDisabledCount: number;
}

export interface MemoryCreateRequest {
  conversationId?: number | null;
  memoryType: MemoryType;
  scope?: MemoryScope;
  subType?: MemorySubType;
  sourceType?: MemorySourceType;
  title?: string;
  reason?: string;
  content: string;
}

export interface MemoryUpdateRequest extends Omit<MemoryCreateRequest, 'conversationId'> {}

export interface MemoryMetadataTypeItem {
  code: MemoryType;
  subTypes: MemorySubType[];
}

export interface MemoryMetadataResponse {
  scopes: MemoryScope[];
  sourceTypes: MemorySourceType[];
  memoryTypes: MemoryMetadataTypeItem[];
}

export type MemoryPage = PageResponse<Memory>;
