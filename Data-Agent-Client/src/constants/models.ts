/**
 * AI Model constants - must match backend ModelEnum.
 * Keep these synchronized with backend enum in ModelEnum.java
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

/** Fallback models list (used when API is unavailable) */
export const FALLBACK_MODELS = [
  { modelName: ModelNames.QWEN3_5_PLUS, supportThinking: false },
  { modelName: ModelNames.QWEN3_MAX, supportThinking: false },
  { modelName: ModelNames.QWEN3_MAX_THINKING, supportThinking: true },
  { modelName: ModelNames.QWEN_PLUS, supportThinking: false },
  { modelName: ModelNames.GLM_5, supportThinking: true },
  { modelName: ModelNames.MINIMAX_M2_5, supportThinking: true },
];
