/**
 * Format tool parameters for display.
 * Attempts to parse as JSON and pretty-print, falls back to raw string.
 */
export function formatParameters(parametersData: string): string {
  if (!parametersData?.trim()) return parametersData;

  try {
    const parsed = JSON.parse(parametersData);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return parametersData;
  }
}
