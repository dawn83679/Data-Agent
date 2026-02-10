import http from '../lib/http';
import type { ChatMessage } from '../types/chat';
import { MessageRole } from '../types/chat';
import type { Conversation, PageResponse } from '../types/conversation';

export const conversationService = {
  /**
   * List conversations with pagination (current user).
   */
  getList: async (params: {
    current?: number;
    size?: number;
  } = {}): Promise<PageResponse<Conversation>> => {
    const response = await http.get<PageResponse<Conversation>>('/conversations', {
      params: { current: params.current ?? 1, size: params.size ?? 10 },
    });
    return response.data;
  },

  /**
   * Get a single conversation by ID (current user).
   */
  getById: async (id: number): Promise<Conversation> => {
    const response = await http.get<Conversation>(`/conversations/${id}`);
    return response.data;
  },

  /**
   * Update conversation title (current user).
   */
  updateTitle: async (id: number, body: { title: string }): Promise<Conversation> => {
    const response = await http.post<Conversation>(`/conversations/${id}`, body);
    return response.data;
  },

  /**
   * Delete a conversation (current user). Also removes all messages under it.
   */
  delete: async (id: number): Promise<void> => {
    await http.delete(`/conversations/${id}`);
  },

  /**
   * Get history messages for a conversation (current user). Returns list compatible with ChatMessage.
   */
  getMessages: async (id: number): Promise<ChatMessage[]> => {
    const response = await http.get<{ id: string; role: string; content: string; blocks?: ChatMessage['blocks']; createdAt?: string }[]>(`/conversations/${id}/messages`);
    const list = Array.isArray(response.data) ? response.data : [];
    return list.map((m) => ({
      id: m.id,
      role: m.role === 'user' ? MessageRole.USER : MessageRole.ASSISTANT,
      content: m.content ?? '',
      blocks: m.blocks,
      createdAt: m.createdAt ? new Date(m.createdAt) : undefined,
    }));
  },
};
