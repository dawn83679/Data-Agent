import { describe, expect, it } from 'vitest';
import { MessageRole } from '../../../types/chat';
import { isHiddenAskUserAnswerMessage, markHiddenAskUserAnswer } from './hiddenAskUserAnswer';

describe('hiddenAskUserAnswer', () => {
  it('detects persisted askUserQuestion answer JSON user messages', () => {
    expect(isHiddenAskUserAnswerMessage({
      role: MessageRole.USER,
      content: JSON.stringify({ type: 'ask_user_question_answer', questions: [] }),
    })).toBe(true);
  });

  it('does not hide normal user JSON messages', () => {
    expect(isHiddenAskUserAnswerMessage({
      role: MessageRole.USER,
      content: JSON.stringify({ type: 'normal_payload' }),
    })).toBe(false);
  });

  it('marks persisted ask-user answers as hidden local boundaries', () => {
    const message = markHiddenAskUserAnswer({
      id: 'msg-1',
      role: MessageRole.USER,
      content: JSON.stringify({ type: 'ask_user_question_answer', questions: [] }),
    });

    expect(message.content).toBe('');
    expect(message.localKind).toBe('hidden-user-boundary');
  });
});
