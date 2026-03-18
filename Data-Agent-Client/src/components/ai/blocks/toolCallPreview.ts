import {
  getToolCallPreviewConfig,
  type ToolCallPreviewFieldKey,
  type ToolCallPreviewRenderPhase,
} from './toolCallPreviewConfig';

export type ToolCallPreviewFieldKind = 'text' | 'code';

export interface ToolCallPreviewField {
  key: string;
  label: string;
  value: string;
  kind?: ToolCallPreviewFieldKind;
}

export interface ToolCallPreview {
  instruction?: string;
  userQuestion?: string;
  connectionIds?: number[];
  databaseName?: string;
  schemaName?: string;
  sql?: string;
  taskInstructions?: string[];
  fields: ToolCallPreviewField[];
}

interface ExtractedTaskPreview {
  connectionIds?: number[];
  taskInstructions?: string[];
}

function parseJsonSafe(raw: string): unknown {
  try {
    return JSON.parse(raw) as unknown;
  } catch {
    return null;
  }
}

function normalizeParsedArgs(parametersData: string): Record<string, unknown> | null {
  if (!parametersData?.trim()) return null;

  let parsed: unknown = parseJsonSafe(parametersData);
  if (typeof parsed === 'string') {
    parsed = parseJsonSafe(parsed);
  }

  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return null;
  }
  return parsed as Record<string, unknown>;
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function uniqueStrings(values: Array<string | undefined>): string[] {
  const seen = new Set<string>();
  const result: string[] = [];

  for (const value of values) {
    const normalized = value?.trim();
    if (!normalized || seen.has(normalized)) continue;
    seen.add(normalized);
    result.push(normalized);
  }

  return result;
}

function uniqueNumbers(values: Array<number | undefined>): number[] {
  const seen = new Set<number>();
  const result: number[] = [];

  for (const value of values) {
    if (value == null || !Number.isFinite(value) || seen.has(value)) continue;
    seen.add(value);
    result.push(value);
  }

  return result;
}

function decodeJsonLikeString(raw: string): string {
  return raw
    .replace(/\\u([0-9a-fA-F]{4})/g, (_, hex: string) => String.fromCharCode(parseInt(hex, 16)))
    .replace(/\\(["\\/bfnrt])/g, (_, char: string) => {
      switch (char) {
        case 'b':
          return '\b';
        case 'f':
          return '\f';
        case 'n':
          return '\n';
        case 'r':
          return '\r';
        case 't':
          return '\t';
        default:
          return char;
      }
    });
}

function readQuotedValue(input: string, startQuoteIndex: number): { value: string; endIndex: number } | null {
  if (input[startQuoteIndex] !== '"') return null;

  let cursor = startQuoteIndex + 1;
  let escaped = false;
  let rawValue = '';

  while (cursor < input.length) {
    const current = input[cursor];
    rawValue += current;

    if (escaped) {
      escaped = false;
      cursor += 1;
      continue;
    }

    if (current === '\\') {
      escaped = true;
      cursor += 1;
      continue;
    }

    if (current === '"') {
      return {
        value: decodeJsonLikeString(rawValue.slice(0, -1)),
        endIndex: cursor + 1,
      };
    }

    cursor += 1;
  }

  return {
    value: decodeJsonLikeString(rawValue),
    endIndex: input.length,
  };
}

function findValueStart(input: string, key: string, fromIndex = 0): { valueStart: number; keyIndex: number } | null {
  const pattern = `"${key}"`;
  let keyIndex = input.indexOf(pattern, fromIndex);

  while (keyIndex >= 0) {
    let cursor = keyIndex + pattern.length;
    while (cursor < input.length && /\s/.test(input[cursor])) cursor += 1;
    if (input[cursor] !== ':') {
      keyIndex = input.indexOf(pattern, keyIndex + pattern.length);
      continue;
    }
    cursor += 1;
    while (cursor < input.length && /\s/.test(input[cursor])) cursor += 1;
    return { valueStart: cursor, keyIndex };
  }

  return null;
}

function extractFirstStringField(input: string, key: string): string | undefined {
  const match = findValueStart(input, key);
  if (!match || input[match.valueStart] !== '"') return undefined;
  return readQuotedValue(input, match.valueStart)?.value.trim() || undefined;
}

function extractAllStringFields(input: string, key: string): string[] {
  const values: string[] = [];
  let cursor = 0;

  while (cursor < input.length) {
    const match = findValueStart(input, key, cursor);
    if (!match) break;
    if (input[match.valueStart] === '"') {
      const value = readQuotedValue(input, match.valueStart);
      if (value?.value.trim()) {
        values.push(value.value.trim());
      }
      cursor = value?.endIndex ?? (match.valueStart + 1);
      continue;
    }
    cursor = match.valueStart + 1;
  }

  return uniqueStrings(values);
}

function extractAllNumberFields(input: string, key: string): number[] {
  const regex = new RegExp(`"${escapeRegExp(key)}"\\s*:\\s*(-?\\d+)`, 'g');
  const values = Array.from(input.matchAll(regex)).map((match) => Number(match[1]));
  return uniqueNumbers(values);
}

function extractTasksFromValue(value: unknown): ExtractedTaskPreview {
  const tasks = Array.isArray(value)
    ? value
    : typeof value === 'string'
      ? (parseJsonSafe(value) as unknown[] | null)
      : null;

  if (!Array.isArray(tasks)) {
    return {};
  }

  const connectionIds = uniqueNumbers(
    tasks.map((task) => (
      task && typeof task === 'object' && 'connectionId' in task
        ? (task as Record<string, unknown>).connectionId as number | undefined
        : undefined
    ))
  );
  const taskInstructions = uniqueStrings(
    tasks.map((task) => (
      task && typeof task === 'object' && 'instruction' in task
        ? (task as Record<string, unknown>).instruction as string | undefined
        : undefined
    ))
  );

  return {
    connectionIds: connectionIds.length > 0 ? connectionIds : undefined,
    taskInstructions: taskInstructions.length > 0 ? taskInstructions : undefined,
  };
}

function extractTasksFromPartialPayload(raw: string | undefined): ExtractedTaskPreview {
  if (!raw) return {};

  return {
    connectionIds: (() => {
      const values = extractAllNumberFields(raw, 'connectionId');
      return values.length > 0 ? values : undefined;
    })(),
    taskInstructions: (() => {
      const values = extractAllStringFields(raw, 'instruction');
      return values.length > 0 ? values : undefined;
    })(),
  };
}

function buildPreviewFields(
  keys: ToolCallPreviewFieldKey[],
  data: {
  instruction?: string;
  userQuestion?: string;
  connectionIds?: number[];
  databaseName?: string;
  schemaName?: string;
  sql?: string;
  taskInstructions?: string[];
}
): ToolCallPreviewField[] {
  const fields: ToolCallPreviewField[] = [];

  for (const key of keys) {
    switch (key) {
      case 'instruction':
        if (data.instruction) {
          fields.push({ key: 'instruction', label: 'Instruction', value: data.instruction });
        }
        break;
      case 'userQuestion':
        if (data.userQuestion) {
          fields.push({ key: 'userQuestion', label: 'Question', value: data.userQuestion });
        }
        break;
      case 'taskInstructions':
        (data.taskInstructions ?? []).forEach((instruction, index) => {
          fields.push({
            key: `task-${index + 1}`,
            label: `Task ${index + 1}`,
            value: instruction,
          });
        });
        break;
      case 'connectionIds':
        if (data.connectionIds?.length) {
          fields.push({
            key: 'connectionIds',
            label: data.connectionIds.length > 1 ? 'Connections' : 'Connection',
            value: data.connectionIds.join(', '),
          });
        }
        break;
      case 'databaseName':
        if (data.databaseName) {
          fields.push({ key: 'databaseName', label: 'Database', value: data.databaseName });
        }
        break;
      case 'schemaName':
        if (data.schemaName) {
          fields.push({ key: 'schemaName', label: 'Schema', value: data.schemaName });
        }
        break;
      case 'sql':
        if (data.sql) {
          fields.push({ key: 'sql', label: 'SQL', value: data.sql, kind: 'code' });
        }
        break;
      default:
        break;
    }
  }

  return fields;
}

export function resolveToolCallPreview(
  toolName: string,
  parametersData: string,
  phase: ToolCallPreviewRenderPhase
): ToolCallPreview | null {
  if (!parametersData?.trim()) return null;
  const config = getToolCallPreviewConfig(toolName);

  const parsed = normalizeParsedArgs(parametersData);
  const parsedTasks = extractTasksFromValue(parsed?.tasks);
  const partialTasks = extractTasksFromPartialPayload(parametersData);

  const directInstruction = typeof parsed?.instruction === 'string'
    ? parsed.instruction.trim()
    : extractFirstStringField(parametersData, 'instruction');
  const directUserQuestion = typeof parsed?.userQuestion === 'string'
    ? parsed.userQuestion.trim()
    : extractFirstStringField(parametersData, 'userQuestion');
  const taskInstructions = uniqueStrings([
    ...(parsedTasks.taskInstructions ?? []),
    ...(partialTasks.taskInstructions ?? []),
  ]);
  const connectionIds = uniqueNumbers([
    ...(parsedTasks.connectionIds ?? []),
    ...(partialTasks.connectionIds ?? []),
    ...extractAllNumberFields(parametersData, 'connectionId'),
  ]);
  const databaseName = typeof parsed?.databaseName === 'string'
    ? parsed.databaseName.trim()
    : extractFirstStringField(parametersData, 'databaseName');
  const schemaName = typeof parsed?.schemaName === 'string'
    ? parsed.schemaName.trim()
    : extractFirstStringField(parametersData, 'schemaName');
  const sql = typeof parsed?.sql === 'string'
    ? parsed.sql.trim()
    : extractFirstStringField(parametersData, 'sql');

  const instruction = directInstruction || taskInstructions[0];
  const userQuestion = directUserQuestion || instruction;
  const preview: ToolCallPreview = {
    instruction: instruction || undefined,
    userQuestion: userQuestion || undefined,
    connectionIds: connectionIds.length > 0 ? connectionIds : undefined,
    databaseName: databaseName || undefined,
    schemaName: schemaName || undefined,
    sql: sql || undefined,
    taskInstructions: taskInstructions.length > 0 ? taskInstructions : undefined,
    fields: [],
  };

  preview.fields = buildPreviewFields(config.fields, preview);

  if (preview.fields.length > 0) {
    return preview;
  }

  if (config.fallbackByPhase[phase] !== 'raw') {
    return null;
  }

  return {
    ...preview,
    fields: [
      {
        key: 'parameters',
        label: config.fallbackLabel ?? 'Parameters',
        value: parametersData,
        kind: 'code',
      },
    ],
  };
}

export const extractToolCallPreview = resolveToolCallPreview;
