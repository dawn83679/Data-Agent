import type { ChatMessage } from '../../../types/chat';
import type { Message } from './types';

/**
 * Convert ChatMessage (API/hook format) to Message (display format).
 * Maps createdAt to timestamp for messageListLib consumers.
 */
export function chatMessageToMessage(msg: ChatMessage): Message {
  return {
    id: msg.id,
    role: msg.role,
    content: msg.content,
    timestamp: msg.createdAt ?? new Date(),
    blocks: msg.blocks,
  };
}

/**
 * Convert ChatMessage[] to Message[].
 */
export function chatMessagesToMessages(msgs: ChatMessage[]): Message[] {
  return msgs.map(chatMessageToMessage);
}
