import type { ModelOption } from '../types/ai';

/**
 * Frontend fallback chat models used when /api/ai/models is unavailable.
 * Keep these aligned with backend chat-visible ai.models.supported entries for graceful degradation.
 */

/** Fallback models list (used when API is unavailable). */
export const FALLBACK_MODELS: ModelOption[] = [
  { modelName: 'qwen3.6-max-preview', supportThinking: true, memoryThreshold: 230000, maxContextTokens: 262144 },
  { modelName: 'qwen3-max-2026-01-23', supportThinking: true, memoryThreshold: 230000, maxContextTokens: 258048 },
];

export const DEFAULT_MODEL = FALLBACK_MODELS[0]?.modelName ?? 'qwen3.6-max-preview';
