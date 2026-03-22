import { MEMORY_DIALOG_MODE } from './memoryPageConstants';
import type {
  MemoryScope,
  MemorySourceType,
  MemorySubType,
  MemoryType,
} from '../../types/memory';

export type MemoryDialogMode = (typeof MEMORY_DIALOG_MODE)[keyof typeof MEMORY_DIALOG_MODE];

export interface FilterFormState {
  keyword: string;
  memoryType: MemoryType | '';
  scope: MemoryScope | '';
  status: string;
}

export interface MemoryFormState {
  conversationId: string;
  memoryType: MemoryType;
  subType: MemorySubType | '';
  scope: MemoryScope;
  sourceType: MemorySourceType;
  title: string;
  reason: string;
  content: string;
  detailJson: string;
  confidenceScore: string;
  salienceScore: string;
  expiresAt: string;
}
