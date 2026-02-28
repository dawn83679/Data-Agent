/**
 * Export query result to CSV and trigger browser download.
 * Used by ResultsPanel for "Export" button.
 */

/**
 * Escape a cell value for CSV: wrap in double quotes if contains comma, newline, or quote.
 */
function escapeCsvCell(value: unknown): string {
  const s = value == null ? '' : String(value);
  const needsQuotes = /[",\r\n]/.test(s);
  if (!needsQuotes) return s;
  return `"${s.replace(/"/g, '""')}"`;
}

/**
 * Build CSV string from headers and rows.
 * Uses UTF-8 BOM so Excel opens it with correct encoding.
 */
export function buildCsvFromResult(
  headers: string[],
  rows: unknown[][]
): string {
  const headerLine = headers.map(escapeCsvCell).join(',');
  const dataLines = rows.map((row) =>
    row.map((cell) => escapeCsvCell(cell)).join(',')
  );
  const lines = [headerLine, ...dataLines];
  const csv = lines.join('\r\n');
  const BOM = '\uFEFF';
  return BOM + csv;
}

/**
 * Default filename with timestamp: export-YYYY-MM-DD-HHmmss.csv
 */
function defaultExportFilename(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  const h = String(now.getHours()).padStart(2, '0');
  const min = String(now.getMinutes()).padStart(2, '0');
  const sec = String(now.getSeconds()).padStart(2, '0');
  return `export-${y}-${m}-${d}-${h}${min}${sec}.csv`;
}

/**
 * Trigger browser download of CSV content as a file.
 * @param content - Full CSV string (can include BOM)
 * @param filename - Optional filename; defaults to export-YYYY-MM-DD-HHmmss.csv
 */
export function downloadCsv(content: string, filename?: string): void {
  const name = filename ?? defaultExportFilename();
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
