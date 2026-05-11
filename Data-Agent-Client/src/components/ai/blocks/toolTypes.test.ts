import { describe, expect, it } from 'vitest';
import { getToolType, ToolType } from './toolTypes';

describe('toolTypes', () => {
  it('renders thinking as a generic visible tool, not a thought block', () => {
    expect(getToolType('thinking')).toBe(ToolType.GENERIC);
  });
});
