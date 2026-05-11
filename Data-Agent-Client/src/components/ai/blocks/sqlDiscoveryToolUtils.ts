const TOOL_DISPLAY_NAMES: Record<string, string> = {
  getConnections: 'Get Connections',
  getDatabases: 'Get Databases',
  getSchemas: 'Get Schemas',
  searchObjects: 'Search Objects',
  getObjectDetail: 'Get Object Detail',
  executeSelectSql: 'Execute Select SQL',
  executeNonSelectSql: 'Execute Write SQL',
  readMemory: 'Read Memory',
  updateMemory: 'Update Memory',
  thinking: 'Thinking',
};

export function getToolDisplayName(toolName: string): string {
  return TOOL_DISPLAY_NAMES[toolName] ?? toolName;
}
