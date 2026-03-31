import { describe, expect, it } from 'vitest';
import {
  getDiscoveryItemLabel,
  getToolDisplayName,
  isSqlDiscoveryListTool,
  parseSqlDiscoveryListResult,
} from './sqlDiscoveryToolUtils';

describe('sqlDiscoveryToolUtils', () => {
  it('recognizes list-style SQL discovery tools', () => {
    expect(isSqlDiscoveryListTool('getDatabases')).toBe(true);
    expect(isSqlDiscoveryListTool('getSchemas')).toBe(true);
    expect(isSqlDiscoveryListTool('searchObjects')).toBe(false);
  });

  it('maps SQL discovery tools to readable labels', () => {
    expect(getToolDisplayName('getDatabases')).toBe('Get Databases');
    expect(getToolDisplayName('getSchemas')).toBe('Get Schemas');
    expect(getToolDisplayName('searchObjects')).toBe('Search Objects');
    expect(getToolDisplayName('unknownTool')).toBe('unknownTool');
  });

  it('maps discovery list tools to their item labels', () => {
    expect(getDiscoveryItemLabel('getDatabases')).toBe('database');
    expect(getDiscoveryItemLabel('getSchemas')).toBe('schema');
  });

  it('parses successful list results', () => {
    const parsed = parseSqlDiscoveryListResult(JSON.stringify({
      success: true,
      message: 'Found 2 database(s) on connection 9.',
      result: ['analytics', 'app'],
    }));

    expect(parsed.success).toBe(true);
    expect(parsed.message).toBe('Found 2 database(s) on connection 9.');
    expect(parsed.items).toEqual(['analytics', 'app']);
  });

  it('treats successful responses without result items as empty lists', () => {
    const parsed = parseSqlDiscoveryListResult(JSON.stringify({
      success: true,
      message: 'No schemas found in database app on connection 9.',
    }));

    expect(parsed.success).toBe(true);
    expect(parsed.items).toEqual([]);
    expect(parsed.message).toContain('No schemas found');
  });

  it('surfaces failed responses without pretending they have data', () => {
    const parsed = parseSqlDiscoveryListResult(JSON.stringify({
      success: false,
      message: 'Failed to get schemas in database app on connection 9.',
    }));

    expect(parsed.success).toBe(false);
    expect(parsed.items).toEqual([]);
    expect(parsed.message).toContain('Failed to get schemas');
  });
});
