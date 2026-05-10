import type { ChatMessage } from '../../../types/chat';
import { MessageRole } from '../../../types/chat';
import type { Message } from './types';

function isAskUserAnswerJson(content: string | null | undefined): boolean {
  const trimmed = (content ?? '').trim();
  if (!trimmed.startsWith('{')) return false;

  try {
    const parsed = JSON.parse(trimmed) as unknown;
    return Boolean(
      parsed
      && typeof parsed === 'object'
      && (parsed as { type?: unknown }).type === 'ask_user_question_answer'
    );
  } catch {
    return false;
  }
}

export function isHiddenAskUserAnswerMessage(message: Pick<Message, 'role' | 'content' | 'localKind'>): boolean {
  return message.localKind === 'hidden-user-boundary'
    || (message.role === MessageRole.USER && isAskUserAnswerJson(message.content));
}

export function markHiddenAskUserAnswer(message: ChatMessage): ChatMessage {
  if (message.localKind != null || message.role !== MessageRole.USER || !isAskUserAnswerJson(message.content)) {
    return message;
  }

  return {
    ...message,
    content: '',
    localKind: 'hidden-user-boundary',
  };
}
