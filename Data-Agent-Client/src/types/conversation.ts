/**
 * Conversation types aligned with backend ConversationResponse / PageResponse.
 */
export interface Conversation {
  id: number;
  title: string | null;
  tokenCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CompactConversationResponse {
  compressed: boolean;
  tokenCountBefore: number | null;
  tokenCountAfter: number | null;
  compressedMessageCount: number;
  keptRecentCount: number;
  summary?: string | null;
}

export interface PageResponse<T> {
  current: number;
  size: number;
  total: number;
  pages: number;
  records: T[];
}
