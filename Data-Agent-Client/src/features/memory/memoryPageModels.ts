import type {
  MemoryScope,
  MemorySourceType,
  MemorySubType,
  MemoryType,
} from '../../types/memory';

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
