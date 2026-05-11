import { describe, expect, it } from 'vitest';
import { getWaitingPrompt } from './PlanningIndicator';

describe('getWaitingPrompt', () => {
  it('advances through translated waiting prompts and stays on the last one', () => {
    expect(getWaitingPrompt('default', 0, ['A', 'B'])).toBe('A');
    expect(getWaitingPrompt('default', 1, ['A', 'B'])).toBe('B');
    expect(getWaitingPrompt('default', 2, ['A', 'B'])).toBe('B');
  });

  it('uses answer-mode fallbacks when translated labels are empty', () => {
    expect(getWaitingPrompt('answer', 0, [])).toBe('模型正在处理你的回答...');
    expect(getWaitingPrompt('answer', 4, [])).toBe('仍在处理中...');
  });
});
