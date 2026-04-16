import type { ModelOption } from '../types/ai';

/**
 * Frontend fallback models used when /api/ai/models is unavailable.
 * Keep these aligned with backend ai.models.supported for graceful degradation.
 */

/** Fallback models list (used when API is unavailable). */
export const FALLBACK_MODELS: ModelOption[] = [
  { modelName: 'qwen3.5-plus', supportThinking: false, memoryThreshold: 900000, maxContextTokens: 1048576 },
  { modelName: 'qwen3.6-plus', supportThinking: false, memoryThreshold: 900000, maxContextTokens: 1000000 },
  { modelName: 'qwen3-max-2026-01-23', supportThinking: false, memoryThreshold: 230000, maxContextTokens: 256000 },
  { modelName: 'qwen3-max-thinking', supportThinking: true, memoryThreshold: 230000, maxContextTokens: 256000 },
];

export const DEFAULT_MODEL = FALLBACK_MODELS[0]?.modelName ?? 'qwen3.5-plus';
