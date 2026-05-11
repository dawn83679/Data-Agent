import { describe, expect, it } from 'vitest';
import { ASK_USER_ANSWER_SUBMIT_OPTIONS } from './AskUserQuestionCard';

describe('ASK_USER_ANSWER_SUBMIT_OPTIONS', () => {
  it('uses answer-specific waiting prompt mode for submitted answers', () => {
    expect(ASK_USER_ANSWER_SUBMIT_OPTIONS).toEqual({
      hideUserMessage: true,
      waitingPromptMode: 'answer',
    });
  });
});
