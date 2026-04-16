import http from '../lib/http';
import { FALLBACK_MODELS } from '../constants/models';
import type { ModelOption } from '../types/ai';

let modelsCache: ModelOption[] | null = null;
let modelsInFlight: Promise<ModelOption[]> | null = null;
const fallbackModelsByName = new Map(FALLBACK_MODELS.map((model) => [model.modelName, model]));

function normalizePositiveNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : null;
}

function normalizeModelOption(input: Partial<ModelOption>): ModelOption {
  const modelName = input.modelName ?? '';
  const fallback = fallbackModelsByName.get(modelName);

  return {
    modelName,
    supportThinking: typeof input.supportThinking === 'boolean'
      ? input.supportThinking
      : Boolean(fallback?.supportThinking),
    memoryThreshold: normalizePositiveNumber(input.memoryThreshold) ?? fallback?.memoryThreshold ?? null,
    maxContextTokens: normalizePositiveNumber(input.maxContextTokens) ?? fallback?.maxContextTokens ?? null,
  };
}

export const aiService = {
  /**
   * Get available chat models (e.g. for model selector).
   * Falls back to empty array on error; caller may use a default list.
   */
  getModels: async (forceRefresh = false): Promise<ModelOption[]> => {
    if (!forceRefresh && modelsCache) {
      return modelsCache;
    }

    if (!forceRefresh && modelsInFlight) {
      return modelsInFlight;
    }

    modelsInFlight = (async () => {
      try {
        const response = await http.get<ModelOption[]>('/ai/models');
        const list = Array.isArray(response.data) ? response.data : [];
        const normalized = list.map((m) => normalizeModelOption(m));
        if (normalized.length > 0) {
          modelsCache = normalized;
        }
        return normalized;
      } catch {
        return [];
      } finally {
        modelsInFlight = null;
      }
    })();

    return modelsInFlight;
  },
};
