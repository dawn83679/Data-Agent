import { FileCode, Table as TableIcon, X, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../lib/utils';
import { useWorkspaceStore } from '../../store/workspaceStore';
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

interface SortableTabProps {
  tabId: string;
  name: string;
  type: 'file' | 'table';
  connectionName?: string;
  databaseName?: string | null;
  isActive: boolean;
  onSwitch: (id: string) => void;
  onClose: (id: string) => void;
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
}: SortableTabProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: tabId });

  const tabLabel = connectionName
    ? `${name} [${connectionName}${databaseName ? ' > ' + databaseName : ''}]`
    : name;

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      onClick={() => onSwitch(tabId)}
      className={cn(
        "flex items-center px-3 py-1.5 text-[11px] rounded-t min-w-[140px] max-w-[240px] group select-none cursor-pointer border-t-2 transition-colors relative",
        isActive
          ? "tab-active border-primary bg-tab-active"
          : "theme-bg-panel theme-text-secondary hover:bg-accent/50 border-transparent",
        isDragging && "opacity-50 bg-blue-100 dark:bg-blue-900"
      )}
      {...attributes}
      {...listeners}
    >
      <span className="mr-2 shrink-0">
        {type === 'file' ? (
          <FileCode className="w-3 h-3 text-blue-400" />
        ) : (
          <TableIcon className="w-3 h-3 text-green-400" />
        )}
      </span>
      <span className="flex-1 truncate mr-4" title={tabLabel}>
        {tabLabel}
      </span>
      <button
        onClick={(e) => {
          e.stopPropagation();
          onClose(tabId);
        }}
        className="absolute right-1.5 p-0.5 rounded opacity-0 group-hover:opacity-100 hover:bg-accent/80 hover:text-red-400 transition-all"
      >
        <X className="w-2.5 h-2.5" />
      </button>
    </div>
  );
}

export function TabBar() {
  const { t } = useTranslation();
  const { tabs, switchTab, closeTab, openTab, reorderTabs } =
    useWorkspaceStore();

  const sensors = useSensors(
    useSensor(PointerSensor, {
      distance: 8,
    } as any)
  );

  const handleAddTab = () => {
    const id = `console-${Date.now()}`;
    openTab({
      id,
      name: 'new_console.sql',
      type: 'file',
      content: '',
    });
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      reorderTabs(String(active.id), String(over.id));
    }
  };

  if (tabs.length === 0) {
    return null;
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragEnd={handleDragEnd}
    >
      <div className="h-9 theme-bg-panel flex items-end space-x-1 overflow-x-auto no-scrollbar border-b theme-border shrink-0">
        <SortableContext
          items={tabs.map(t => t.id)}
          strategy={horizontalListSortingStrategy}
        >
          {tabs.map((tab) => (
            <SortableTab
              key={tab.id}
              tabId={tab.id}
              name={tab.name}
              type={tab.type}
              connectionName={tab.metadata?.connectionName}
              databaseName={tab.metadata?.databaseName}
              isActive={tab.active}
              onSwitch={switchTab}
              onClose={closeTab}
            />
          ))}
        </SortableContext>

        <button
          onClick={handleAddTab}
          className="flex items-center justify-center w-8 h-8 mb-0.5 theme-text-secondary hover:text-blue-500 transition-colors shrink-0"
          title={t('workspace.new_console_tab_title')}
        >
          <Plus className="w-3.5 h-3.5" />
        </button>
      </div>
    </DndContext>
  );
}
