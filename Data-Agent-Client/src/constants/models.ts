import type { ModelOption } from '../types/ai';

/**
 * AI Model constants - must match backend ModelEnum.
 * Keep these synchronized with backend enum in ModelEnum.java for fallback mode.
 */

/** Model names (synchronized with backend ModelEnum) */
export const ModelNames = {
  QWEN3_5_PLUS: 'qwen3.5-plus',
  QWEN3_MAX: 'qwen3-max',
  QWEN3_MAX_THINKING: 'qwen3-max-thinking',
  QWEN_PLUS: 'qwen-plus',
  GLM_5: 'glm-5',
  MINIMAX_M2_5: 'MiniMax-M2.5',
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
  { modelName: ModelNames.GLM_5, supportThinking: true, memoryThreshold: 128000, maxContextTokens: 200000 },
  { modelName: ModelNames.MINIMAX_M2_5, supportThinking: true, memoryThreshold: 150000, maxContextTokens: 204800 },
];
