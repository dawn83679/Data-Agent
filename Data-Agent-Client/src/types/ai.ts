/** Option for chat model selection (matches backend ModelOptionResponse). */
export interface ModelOption {
  modelName: string;
  supportThinking: boolean;
  memoryThreshold: number | null;
  maxContextTokens: number | null;
}
