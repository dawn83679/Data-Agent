import { Braces, Database, FileCode, ListTodo, Table as TableIcon, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../lib/utils';
import { useTabStore } from '../../store/tabStore';
import { useWorkspaceStore } from '../../store/workspaceStore';
import { I18N_KEYS } from '../../constants/i18nKeys';
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  SortableContext,
  horizontalListSortingStrategy,
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuSeparator,
  ContextMenuTrigger,
} from '../ui/ContextMenu';
import { Tooltip, TooltipTrigger, TooltipContent, TooltipProvider } from '../ui/Tooltip';
import type { SubAgentConsoleTabMetadata } from '../../types/tab';
import { SUB_AGENT_TYPES, type SubAgentType } from '../ai/blocks/subAgentTypes';

interface SortableTabProps {
  tabId: string;
  name: string;
  type: 'file' | 'table' | 'plan' | 'subagent-console';
  connectionName?: string;
  databaseName?: string | null;
  isActive: boolean;
  onSwitch: (id: string) => void;
  onClose: (id: string) => void;
  onCloseLeft: (id: string) => void;
  onCloseRight: (id: string) => void;
  onCloseOthers: (id: string) => void;
  onCloseAll: () => void;
  subAgentType?: SubAgentType;
  subAgentStatus?: SubAgentConsoleTabMetadata['status'];
}

function SortableTab({
  tabId,
  name,
  type,
  connectionName,
  databaseName,
  isActive,
  onSwitch,
  onClose,
  onCloseLeft,
  onCloseRight,
  onCloseOthers,
  onCloseAll,
  subAgentType,
  subAgentStatus,
}: SortableTabProps) {
  const { t } = useTranslation();
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: tabId });

  const tabLabel =
    type === 'plan' || type === 'subagent-console'
      ? name
      : type === 'table'
        ? name
        : connectionName
          ? `${connectionName}${databaseName ? '_' + databaseName : ''}`
          : name;

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  const isPlannerConsole = type === 'subagent-console' && subAgentType === SUB_AGENT_TYPES.PLANNER;
  const isFailedSubAgent = type === 'subagent-console' && subAgentStatus === 'error';
  const activeClass = isFailedSubAgent
    ? 'workbench-tab-chip--error shadow-sm'
    : isActive
      ? isPlannerConsole
        ? 'border-purple-400/40 bg-purple-500/10 text-[color:var(--text-primary)] shadow-sm'
        : type === 'subagent-console'
          ? 'border-cyan-400/40 bg-cyan-500/10 text-[color:var(--text-primary)] shadow-sm'
          : 'workbench-chip--active text-[color:var(--text-primary)]'
      : 'theme-text-secondary hover:theme-text-primary';

  const subAgentIconClass = isFailedSubAgent
    ? 'text-white'
    : isPlannerConsole
      ? 'text-purple-400'
      : 'text-cyan-400';
  const labelClass = isFailedSubAgent
    ? 'text-white font-semibold'
    : '';
  const closeButtonClass = isFailedSubAgent
    ? 'text-white/85 hover:bg-white/15 hover:text-white'
    : '';

  return (
    <ContextMenu>
      <Tooltip>
        <TooltipTrigger asChild>
          <ContextMenuTrigger asChild>
            <div
              ref={setNodeRef}
              style={style}
              onClick={() => onSwitch(tabId)}
              data-active={isActive ? 'true' : undefined}
              data-error={isFailedSubAgent ? 'true' : undefined}
              className={cn(
                'group workbench-chip workbench-tab-chip',
                activeClass,
                isDragging && 'opacity-40'
              )}
              {...attributes}
              {...listeners}
            >
              <span className="mr-1.5 shrink-0">
                {type === 'plan' ? (
                  <ListTodo className="w-3 h-3 text-amber-400" />
                ) : type === 'subagent-console' ? (
                  isPlannerConsole ? (
                    <Braces className={cn('w-3 h-3', subAgentIconClass)} />
                  ) : (
                    <Database className={cn('w-3 h-3', subAgentIconClass)} />
                  )
                ) : type === 'file' ? (
                  <FileCode className="w-3 h-3 text-blue-400" />
                ) : (
                  <TableIcon className="w-3 h-3 text-green-400" />
                )}
              </span>
              <span className={cn('flex-1 truncate mr-1', labelClass)}>
                {tabLabel}
              </span>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  e.preventDefault();
                  onClose(tabId);
                }}
                onPointerDown={(e) => e.stopPropagation()}
                className={cn(
                  'workbench-icon-button h-5 w-5 p-0 opacity-0 group-hover:opacity-100 transition-all cursor-pointer flex-shrink-0',
                  isFailedSubAgent
                    ? closeButtonClass
                    : 'hover:bg-red-500/20 hover:text-red-400'
                )}
                type="button"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          </ContextMenuTrigger>
        </TooltipTrigger>
        <TooltipContent side="bottom" className="text-xs">{tabLabel}</TooltipContent>
      </Tooltip>
      <ContextMenuContent className="min-w-[180px]">
        <ContextMenuItem className="text-xs" onClick={() => onClose(tabId)}>
          {t(I18N_KEYS.WORKSPACE.TAB_CLOSE)}
        </ContextMenuItem>
        <ContextMenuSeparator />
        <ContextMenuItem className="text-xs" onClick={() => onCloseLeft(tabId)}>
          {t(I18N_KEYS.WORKSPACE.TAB_CLOSE_LEFT)}
        </ContextMenuItem>
        <ContextMenuItem className="text-xs" onClick={() => onCloseRight(tabId)}>
          {t(I18N_KEYS.WORKSPACE.TAB_CLOSE_RIGHT)}
        </ContextMenuItem>
        <ContextMenuSeparator />
        <ContextMenuItem className="text-xs" onClick={() => onCloseOthers(tabId)}>
          {t(I18N_KEYS.WORKSPACE.TAB_CLOSE_OTHERS)}
        </ContextMenuItem>
        <ContextMenuItem className="text-xs" onClick={onCloseAll}>
          {t(I18N_KEYS.WORKSPACE.TAB_CLOSE_ALL)}
        </ContextMenuItem>
      </ContextMenuContent>
    </ContextMenu>
  );
}

export function TabBar() {
  const { tabs } = useTabStore();
  const { switchTab, closeTab, closeTabsToLeft, closeTabsToRight, closeOtherTabs, closeAllTabs, reorderTabs } =
    useWorkspaceStore();

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } } as any)
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (over && active.id !== over.id) {
      reorderTabs(String(active.id), String(over.id));
    }
  };

  if (tabs.length === 0) return null;

  return (
    <TooltipProvider>
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <div className="workbench-header flex h-[46px] items-stretch overflow-x-auto no-scrollbar px-2.5 py-2 shrink-0">
          <SortableContext
            items={tabs.map((tab) => tab.id)}
            strategy={horizontalListSortingStrategy}
          >
            {tabs.map((tab) => (
              <SortableTab
                key={tab.id}
                tabId={tab.id}
                name={tab.name}
                type={tab.type}
                connectionName={tab.type !== 'plan' && tab.type !== 'subagent-console' ? (tab.metadata as import('../../types/tab').ConsoleTabMetadata | undefined)?.connectionName : undefined}
                databaseName={tab.type !== 'plan' && tab.type !== 'subagent-console' ? (tab.metadata as import('../../types/tab').ConsoleTabMetadata | undefined)?.databaseName : undefined}
                subAgentType={tab.type === 'subagent-console' ? (tab.metadata as SubAgentConsoleTabMetadata | undefined)?.agentType : undefined}
                subAgentStatus={tab.type === 'subagent-console' ? (tab.metadata as SubAgentConsoleTabMetadata | undefined)?.status : undefined}
                isActive={tab.active}
                onSwitch={switchTab}
                onClose={closeTab}
                onCloseLeft={closeTabsToLeft}
                onCloseRight={closeTabsToRight}
                onCloseOthers={closeOtherTabs}
                onCloseAll={closeAllTabs}
              />
            ))}
          </SortableContext>
        </div>
      </DndContext>
    </TooltipProvider>
  );
}
