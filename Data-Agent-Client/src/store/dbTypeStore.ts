import { create } from 'zustand';
import type { DbTypeOption } from '../types/dbType';
import { dbTypeService } from '../services/dbType.service';

interface DbTypeState {
  supportedDbTypes: DbTypeOption[];
  isLoading: boolean;
  fetchSupportedDbTypes: () => Promise<void>;
}

export const useDbTypeStore = create<DbTypeState>((set, get) => ({
  supportedDbTypes: [],
  isLoading: false,

  fetchSupportedDbTypes: async () => {
    const state = get();
    if (state.isLoading || state.supportedDbTypes.length > 0) return;

    set({ isLoading: true });
    try {
      const data = await dbTypeService.getSupportedDbTypes();
      set({ supportedDbTypes: data || [] });
    } catch (error) {
      console.error('Failed to fetch supported db types:', error);
    } finally {
      set({ isLoading: false });
    }
  },
}));
