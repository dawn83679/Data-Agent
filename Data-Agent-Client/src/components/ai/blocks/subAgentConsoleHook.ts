import { useEffect, useRef } from 'react';
import { useWorkspaceStore } from '../../../store/workspaceStore';
import type { SubAgentConsoleTabMetadata, SubAgentInvocation } from '../../../types/tab';
import { subAgentConsoleTabId } from './subAgentDataHelpers';
import type { SubAgentType } from './subAgentTypes';

export interface UseSubAgentConsoleTabOptions {
  enabled?: boolean;
  toolCallId?: string;
  taskKey?: string;
  conversationId: number | null;
  agentType: SubAgentType;
  taskLabel: string;
  status: 'running' | 'complete' | 'error';
  startedAt: number;
  completedAt?: number;
  params?: SubAgentConsoleTabMetadata['params'];
  summary?: string;
  resultJson?: string;
  invocations: SubAgentInvocation[];
}

export function useSubAgentConsoleTab(options: UseSubAgentConsoleTabOptions) {
  const {
    enabled = true,
    toolCallId,
    taskKey,
    conversationId,
    agentType,
    taskLabel,
    status,
    startedAt,
    completedAt,
    params,
    summary,
    resultJson,
    invocations,
  } = options;

  const lastMetadataKeyRef = useRef('');
  const fallbackToolCallIdRef = useRef(`subagent-${Date.now()}`);
  const stableToolCallId = toolCallId ?? fallbackToolCallIdRef.current;
  const tabId = subAgentConsoleTabId(stableToolCallId, taskKey);
  const metadataRef = useRef<SubAgentConsoleTabMetadata | null>(null);
  const autoOpenedRef = useRef(false);
  const metadata: SubAgentConsoleTabMetadata = {
    conversationId,
    agentType,
    status,
    startedAt,
    completedAt,
    params,
    summary,
    resultJson,
    invocations,
  };

  useEffect(() => {
    if (!enabled) return;
    const ws = useWorkspaceStore.getState();
    metadataRef.current = metadata;

    const metadataKey = [
      status,
      completedAt ?? '',
      summary ?? '',
      resultJson ?? '',
      ...invocations.map((invocation) => [
        invocation.id,
        invocation.status,
        invocation.resultSummary ?? '',
        invocation.resultJson ?? '',
        invocation.nestedToolRuns?.map((segment) => {
          if (segment.kind !== 'TOOL_RUN') return segment.kind;
          return [
            segment.toolName,
            segment.parametersData,
            segment.responseData,
            segment.responseError ? 'error' : '',
            segment.executionState ?? '',
          ].join('|');
        }).join('~') ?? '',
        invocation.nestedToolCalls?.map((tool) => `${tool.toolName}:${tool.status}`).join('|') ?? '',
      ].join(':')),
    ].join('||');

    if (metadataKey === lastMetadataKeyRef.current) return;
    lastMetadataKeyRef.current = metadataKey;

    if (status === 'running') {
      ws.tabs
        .filter((tab) => {
          if (tab.id === tabId || tab.type !== 'subagent-console') {
            return false;
          }
          const tabMetadata = tab.metadata as SubAgentConsoleTabMetadata | undefined;
          return tabMetadata?.agentType === agentType && tabMetadata.status === 'error';
        })
        .forEach((tab) => {
          ws.closeTab(tab.id);
        });
    }

    const existingTab = ws.tabs.find((tab) => tab.id === tabId);
    if (existingTab) {
      ws.updateSubAgentConsole(tabId, metadata);
      return;
    }

    if (status === 'running' && !autoOpenedRef.current) {
      autoOpenedRef.current = true;
      ws.openTab({
        id: tabId,
        name: `${taskLabel} Console`,
        type: 'subagent-console',
        metadata,
      });
    }
  }, [agentType, completedAt, conversationId, enabled, invocations, params, resultJson, stableToolCallId, startedAt, status, summary, tabId, taskKey, taskLabel]);

  const handleOpenConsole = () => {
    if (!enabled) return;
    const ws = useWorkspaceStore.getState();
    const existingTab = ws.tabs.find((tab) => tab.id === tabId);
    if (existingTab) {
      ws.updateSubAgentConsole(tabId, metadata);
      ws.switchTab(tabId);
      return;
    }
    ws.openTab({
      id: tabId,
      name: `${taskLabel} Console`,
      type: 'subagent-console',
      metadata,
    });
  };

  return { tabId, handleOpenConsole };
}
