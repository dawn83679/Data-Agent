import http from '../lib/http';
import type { ModelOption } from '../types/ai';

export const aiService = {
  /**
   * Get available chat models (e.g. for model selector).
   * Falls back to empty array on error; caller may use a default list.
   */
  getModels: async (): Promise<ModelOption[]> => {
    try {
      const response = await http.get<ModelOption[]>('/ai/models');
      const list = Array.isArray(response.data) ? response.data : [];
      return list.map((m) => ({
        modelName: m.modelName ?? '',
        supportThinking: Boolean(m.supportThinking),
      }));
    } catch {
      return [];
    }
  },
};
