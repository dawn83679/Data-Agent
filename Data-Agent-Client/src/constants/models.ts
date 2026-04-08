import type { ModelOption } from '../types/ai';

/**
<<<<<<< HEAD
 * Frontend fallback models used when /api/ai/models is unavailable.
 * Keep these aligned with backend ai.models.supported for graceful degradation.
 */

/** Fallback models list (used when API is unavailable). */
export const FALLBACK_MODELS: ModelOption[] = [
  { modelName: 'qwen3.5-plus', supportThinking: false, memoryThreshold: 900000, maxContextTokens: 1048576 },
  { modelName: 'qwen3-max-2026-01-23', supportThinking: false, memoryThreshold: 230000, maxContextTokens: 256000 },
  { modelName: 'qwen3-max-thinking', supportThinking: true, memoryThreshold: 230000, maxContextTokens: 256000 },
=======
 * AI Model constants - must match backend ModelEnum.
 * Keep these synchronized with backend enum in ModelEnum.java for fallback mode.
 */

/** Model names (synchronized with backend ModelEnum) */
export const ModelNames = {
  QWEN3_5_PLUS: 'qwen3.5-plus',
  QWEN3_MAX: 'qwen3-max-2026-01-23',
  QWEN3_MAX_THINKING: 'qwen3-max-thinking',
  QWEN_PLUS: 'qwen-plus',
} as const;

export type ModelName = (typeof ModelNames)[keyof typeof ModelNames];

/** Default model for initial state */
export const DEFAULT_MODEL = ModelNames.QWEN3_5_PLUS;

/** Fallback models list (used when API is unavailable). */
export const FALLBACK_MODELS: ModelOption[] = [
  { modelName: ModelNames.QWEN3_5_PLUS, supportThinking: false, memoryThreshold: 900000, maxContextTokens: 1048576 },
  { modelName: ModelNames.QWEN3_MAX, supportThinking: false, memoryThreshold: 230000, maxContextTokens: 256000 },
  { modelName: ModelNames.QWEN3_MAX_THINKING, supportThinking: true, memoryThreshold: 230000, maxContextTokens: 256000 },
  { modelName: ModelNames.QWEN_PLUS, supportThinking: false, memoryThreshold: 900000, maxContextTokens: 1048576 },
>>>>>>> 55de6b9b235ffd91a8c266a1c07a27b7fb059793
];

export const DEFAULT_MODEL = FALLBACK_MODELS[0]?.modelName ?? 'qwen3.5-plus';
