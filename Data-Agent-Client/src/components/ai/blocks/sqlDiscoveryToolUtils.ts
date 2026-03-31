const SQL_DISCOVERY_LIST_TOOL_NAMES = new Set(['getDatabases', 'getSchemas']);

const TOOL_DISPLAY_NAMES: Record<string, string> = {
  getDatabases: 'Get Databases',
  getSchemas: 'Get Schemas',
  searchObjects: 'Search Objects',
  getObjectDetail: 'Get Object Detail',
  executeSelectSql: 'Execute Select SQL',
};

export interface SqlDiscoveryListResult {
  success: boolean;
  message: string;
  items: string[];
  prettyJson: string;
}

function parseJsonSafe(raw: string): unknown {
  try {
    return JSON.parse(raw) as unknown;
  } catch {
    return null;
  }
}

function normalizeParsedResult(responseData: string): Record<string, unknown> | null {
  if (!responseData.trim()) return null;

  let parsed: unknown = parseJsonSafe(responseData);
  if (typeof parsed === 'string') {
    parsed = parseJsonSafe(parsed) ?? parsed;
  }

  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return null;
  }

  return parsed as Record<string, unknown>;
}

export function isSqlDiscoveryListTool(toolName: string): boolean {
  return SQL_DISCOVERY_LIST_TOOL_NAMES.has(toolName);
}

export function getToolDisplayName(toolName: string): string {
  return TOOL_DISPLAY_NAMES[toolName] ?? toolName;
}

export function getDiscoveryItemLabel(toolName: string): string {
  if (toolName === 'getSchemas') return 'schema';
  return 'database';
}

export function parseSqlDiscoveryListResult(responseData: string): SqlDiscoveryListResult {
  const parsed = normalizeParsedResult(responseData);
  const rawItems = Array.isArray(parsed?.result) ? parsed.result : [];
  const items = rawItems
    .map((item) => {
      if (typeof item === 'string') return item.trim();
      if (item == null) return '';
      return String(item).trim();
    })
    .filter((item) => item.length > 0);

  return {
    success: typeof parsed?.success === 'boolean' ? parsed.success : true,
    message: typeof parsed?.message === 'string' ? parsed.message : '',
    items,
    prettyJson: parsed ? JSON.stringify(parsed, null, 2) : (responseData || ''),
  };
}
