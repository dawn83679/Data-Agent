interface NamedObjectDetailPayload {
  objectName?: unknown;
  objectType?: unknown;
  connectionId?: unknown;
  databaseName?: unknown;
  schemaName?: unknown;
  success?: unknown;
  error?: unknown;
  ddl?: unknown;
  rowCount?: unknown;
  indexes?: unknown;
}

interface AgentToolResultWrapper {
  success?: unknown;
  message?: unknown;
  result?: unknown;
}

export interface GetObjectDetailItemView {
  key: string;
  connectionId?: number;
  objectName: string;
  objectType?: string;
  databaseName?: string;
  schemaName?: string;
  success: boolean;
  error?: string;
  ddl?: string;
  rowCount?: number;
  indexesCount: number;
}

export interface GetObjectDetailPayloadView {
  items: GetObjectDetailItemView[];
  prettyJson: string;
}

function parseJsonSafe<T>(raw: string): T | null {
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function normalizeResultPayload(value: unknown): unknown {
  if (typeof value === 'string') {
    return parseJsonSafe<unknown>(value) ?? value;
  }
  return value;
}

function toOptionalString(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() !== '' ? value.trim() : undefined;
}

function toOptionalNumber(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

export function parseGetObjectDetailResponse(responseData: string): GetObjectDetailPayloadView {
  const parsed = parseJsonSafe<unknown>(responseData);
  const prettyJson = parsed != null
    ? JSON.stringify(parsed, null, 2)
    : (responseData || '—');

  const payload = parsed && typeof parsed === 'object' && !Array.isArray(parsed)
    ? normalizeResultPayload((parsed as AgentToolResultWrapper).result)
    : parsed;

  if (!Array.isArray(payload)) {
    return { items: [], prettyJson };
  }

  const rawItems = payload.filter((item): item is NamedObjectDetailPayload => !!item && typeof item === 'object' && !Array.isArray(item));

  const items = rawItems.map<GetObjectDetailItemView>((item, index) => ({
    key: `detail-${index}`,
    connectionId: toOptionalNumber(item.connectionId),
    objectName: toOptionalString(item.objectName) ?? '',
    objectType: toOptionalString(item.objectType),
    databaseName: toOptionalString(item.databaseName),
    schemaName: toOptionalString(item.schemaName),
    success: item.success === true,
    error: toOptionalString(item.error),
    ddl: toOptionalString(item.ddl),
    rowCount: toOptionalNumber(item.rowCount),
    indexesCount: Array.isArray(item.indexes) ? item.indexes.length : 0,
  }));

  return { items, prettyJson };
}
