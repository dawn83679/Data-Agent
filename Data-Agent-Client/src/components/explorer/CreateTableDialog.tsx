import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Plus,
  Trash2,
  ChevronUp,
  ChevronDown,
  FileText,
  ChevronLeft,
  ChevronRight,
  Settings,
  CheckCircle2,
  HelpCircle,
  ChevronDown as ChevronDownIcon,
  Copy,
  ClipboardPaste,
  MoreVertical,
  Square,
  Key,
  Link2,
  List,
  CheckSquare,
  Box,
  Layers,
} from 'lucide-react';
import {
  Dialog,
  DialogPortal,
  DialogOverlay,
  DialogClose,
} from '../ui/Dialog';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { I18N_KEYS } from '../../constants/i18nKeys';
import type { ExplorerNode } from '../../types/explorer';
import type { DbConnection } from '../../types/connection';
import { cn } from '../../lib/utils';
import { useCreateTableDialogState, type TreeSectionId } from './useCreateTableDialogState';

interface CreateTableDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  node: ExplorerNode | null;
  connections?: DbConnection[];
  onSuccess?: (node: ExplorerNode) => void;
}

const TREE_ICONS: Record<TreeSectionId, ReactNode> = {
  columns: <Square className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  keys: <Key className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  foreign_keys: <Link2 className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  indexes: <List className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  checks: <CheckSquare className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  virtual_columns: <Box className="h-3.5 w-3.5 shrink-0 opacity-70" />,
  virtual_fk: <Layers className="h-3.5 w-3.5 shrink-0 opacity-70" />,
};

const TREE_SECTIONS: { id: TreeSectionId; labelKey: string; enabled: boolean }[] = [
  { id: 'columns', labelKey: 'explorer.create_dialog_columns', enabled: true },
  { id: 'keys', labelKey: 'explorer.create_dialog_keys', enabled: true },
  { id: 'foreign_keys', labelKey: 'explorer.create_dialog_foreign_keys', enabled: true },
  { id: 'indexes', labelKey: 'explorer.create_dialog_indexes', enabled: true },
  { id: 'checks', labelKey: 'explorer.create_dialog_checks', enabled: false },
  { id: 'virtual_columns', labelKey: 'explorer.create_dialog_virtual_columns', enabled: false },
  { id: 'virtual_fk', labelKey: 'explorer.create_dialog_virtual_fk', enabled: false },
];

export function CreateTableDialog({
  open,
  onOpenChange,
  node,
  connections = [],
  onSuccess,
}: CreateTableDialogProps) {
  const { t } = useTranslation();
  const {
    MIN_WIDTH,
    MIN_HEIGHT,
    COMMON_TYPES,
    formScrollRef,
    handleFormWheel,
    tableName,
    setTableName,
    tableComment,
    setTableComment,
    selectedSection,
    setSelectedSection,
    checkedSections,
    setCheckedSections,
    columns,
    foreignKeys,
    indexes,
    previewExpanded,
    setPreviewExpanded,
    isPending,
    leftRootExpanded,
    setLeftRootExpanded,
    position,
    size,
    handleHeaderMouseDown,
    handleResizeMouseDown,
    connectionId,
    displayTableName,
    rootLabel,
    previewSql,
    handleClose,
    addColumn,
    removeColumn,
    updateColumn,
    addForeignKey,
    removeForeignKey,
    updateForeignKey,
    addIndex,
    removeIndex,
    updateIndex,
    moveColumn,
    handleSubmit,
  } = useCreateTableDialogState({
    open,
    onOpenChange,
    node,
    connections,
    onSuccess,
  });

  if (!node) return null;

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogPortal>
        <DialogOverlay />
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="create-table-title"
          className={cn(
            'fixed z-50 flex flex-col overflow-hidden rounded-lg border border-border bg-background shadow-lg',
            'data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0',
          )}
          style={{
            left: position.x,
            top: position.y,
            width: size.width,
            height: size.height,
            minWidth: MIN_WIDTH,
            minHeight: MIN_HEIGHT,
          }}
        >
          <div
            className="flex shrink-0 select-none items-center justify-between border-b border-border px-4 py-2.5 pr-12"
            onMouseDown={handleHeaderMouseDown}
          >
            <h2 id="create-table-title" className="text-base font-semibold">
              {t('explorer.create_dialog_title')}
            </h2>
            <DialogClose asChild>
              <Button variant="ghost" size="icon" className="absolute right-2 top-2.5 h-8 w-8 rounded-sm opacity-70 hover:opacity-100">
                <span className="sr-only">Close</span>
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18 6 6 18" />
                  <path d="m6 6 12 12" />
                </svg>
              </Button>
            </DialogClose>
          </div>

          <div className="flex shrink-0 items-center gap-0.5 border-b border-border bg-muted/5 px-3 py-1.5">
            <Button variant="ghost" size="icon" className="-ml-1 h-8 w-8 rounded" onClick={addColumn} title={t(I18N_KEYS.EXPLORER.CREATE_TABLE_ADD_COLUMN)}>
              <Plus className="h-5 w-5" />
            </Button>
            <div className="mx-0.5 h-5 w-px bg-border" />
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title={t(I18N_KEYS.EXPLORER.CREATE_TABLE_REMOVE_COLUMN)}>
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title="复制">
              <Copy className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title="粘贴">
              <ClipboardPaste className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title="上移">
              <ChevronUp className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded" title="下移">
              <ChevronDown className="h-3.5 w-3.5" />
            </Button>
            <div className="mx-0.5 h-5 w-px bg-border" />
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded">
              <ChevronLeft className="h-3.5 w-3.5" />
            </Button>
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded">
              <ChevronRight className="h-3.5 w-3.5" />
            </Button>
            <div className="ml-3 flex items-center gap-1.5">
              <FileText className="h-4 w-4 shrink-0 text-muted-foreground" />
              <span className="text-sm text-foreground">{displayTableName}</span>
            </div>
            <div className="min-w-2 flex-1" />
            <Button variant="ghost" size="icon" className="h-7 w-7 rounded">
              <MoreVertical className="h-4 w-4" />
            </Button>
          </div>

          <div className="create-table-dialog-form flex min-h-0 flex-1 overflow-hidden">
            <div className="flex w-[220px] shrink-0 flex-col border-r border-border bg-muted/5">
              <button
                type="button"
                className="flex w-full items-center gap-1.5 truncate px-3 py-2 text-left text-xs font-medium text-muted-foreground hover:bg-muted/50"
                onClick={() => setLeftRootExpanded((expanded) => !expanded)}
              >
                <ChevronDownIcon className={cn('h-4 w-4 shrink-0 transition-transform', !leftRootExpanded && '-rotate-90')} />
                <span className="truncate" title={rootLabel}>{rootLabel}</span>
              </button>
              {leftRootExpanded ? (
                <div className="flex-1 overflow-y-auto px-1 py-1 text-xs">
                  {TREE_SECTIONS.map((section) => (
                    <div
                      key={section.id}
                      className={cn(
                        'flex cursor-default items-center gap-2 rounded px-2 py-1.5',
                        selectedSection === section.id ? 'bg-primary/15 text-foreground' : '',
                        !section.enabled ? 'opacity-50' : 'hover:bg-muted/80',
                      )}
                      onClick={() => section.enabled && setSelectedSection(section.id)}
                    >
                      <input
                        type="checkbox"
                        checked={checkedSections.has(section.id)}
                        onChange={(event) => {
                          event.stopPropagation();
                          setCheckedSections((prev) => {
                            const next = new Set(prev);
                            if (next.has(section.id)) next.delete(section.id);
                            else next.add(section.id);
                            return next;
                          });
                        }}
                        onClick={(event) => event.stopPropagation()}
                      />
                      {TREE_ICONS[section.id]}
                      <span>{t(section.labelKey)}</span>
                    </div>
                  ))}
                </div>
              ) : null}
            </div>

            <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
              <div className="flex items-center gap-2 border-b border-border px-4 py-3">
                <FileText className="h-4 w-4 shrink-0 text-muted-foreground" />
                <span className="text-sm font-medium">{displayTableName}</span>
              </div>
              <div
                className="flex-1 overflow-y-auto overflow-x-hidden overscroll-contain p-3"
                ref={formScrollRef}
                onWheel={handleFormWheel}
              >
                <div className="space-y-2">
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_name')}</label>
                    <div className="flex items-center gap-1">
                      <Input
                        value={tableName}
                        onChange={(event) => setTableName(event.target.value)}
                        placeholder={t(I18N_KEYS.EXPLORER.CREATE_TABLE_NAME_PLACEHOLDER)}
                        className="h-8 flex-1 text-sm"
                      />
                      <Button variant="ghost" size="icon" className="h-8 w-8 shrink-0">...</Button>
                    </div>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_comment')}</label>
                    <Input
                      value={tableComment}
                      onChange={(event) => setTableComment(event.target.value)}
                      className="h-8 text-sm"
                    />
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_persistence')}</label>
                    <div className="flex items-center gap-2">
                      <select className="h-8 flex-1 rounded-md border px-2 text-sm">
                        <option value="PERSISTENT">PERSISTENT</option>
                        <option value="UNLOGGED">UNLOGGED</option>
                        <option value="TEMPORARY">TEMPORARY</option>
                      </select>
                      <label className="flex shrink-0 items-center gap-1.5 text-xs">
                        <input type="checkbox" className="rounded border border-border bg-muted/80 accent-primary" />
                        {t('explorer.create_dialog_has_oid')}
                      </label>
                    </div>
                  </div>

                  {selectedSection === 'columns' ? (
                    <div className="grid grid-cols-[100px_1fr] items-start gap-x-3 gap-y-1">
                      <label className="pt-1.5 text-xs leading-8 text-muted-foreground">
                        {t(I18N_KEYS.EXPLORER.CREATE_TABLE_COLUMNS)}
                      </label>
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <Button type="button" variant="outline" size="sm" className="h-8 shrink-0 text-xs" onClick={addColumn}>
                            <Plus className="mr-1 h-3.5 w-3.5" />
                            {t(I18N_KEYS.EXPLORER.CREATE_TABLE_ADD_COLUMN)}
                          </Button>
                        </div>
                        <div className="max-h-44 overflow-y-auto rounded-md border border-input divide-y divide-border">
                          {columns.map((col, index) => (
                            <div key={index} className="flex min-h-[36px] items-center gap-2 bg-muted/10 px-2 py-1">
                              <Input
                                value={col.name}
                                onChange={(event) => updateColumn(index, 'name', event.target.value)}
                                placeholder={t(I18N_KEYS.EXPLORER.CREATE_TABLE_COLUMN_NAME)}
                                className="h-8 min-w-0 flex-1 shrink-0 text-sm"
                              />
                              <select
                                value={col.type}
                                onChange={(event) => updateColumn(index, 'type', event.target.value)}
                                className="h-8 min-w-[110px] shrink-0 rounded-md border px-2.5 text-sm"
                              >
                                {COMMON_TYPES.map((type) => (
                                  <option key={type} value={type}>{type}</option>
                                ))}
                              </select>
                              <label className="flex shrink-0 cursor-default items-center gap-1.5 text-sm">
                                <input
                                  type="checkbox"
                                  checked={col.nullable}
                                  onChange={(event) => updateColumn(index, 'nullable', event.target.checked)}
                                />
                                {t(I18N_KEYS.EXPLORER.CREATE_TABLE_COLUMN_NULLABLE)}
                              </label>
                              <div className="flex shrink-0 items-center border-l border-border pl-1">
                                <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => moveColumn(index, 'up')} disabled={index === 0}>
                                  <ChevronUp className="h-3.5 w-3.5" />
                                </Button>
                                <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => moveColumn(index, 'down')} disabled={index === columns.length - 1}>
                                  <ChevronDown className="h-3.5 w-3.5" />
                                </Button>
                              </div>
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 shrink-0 text-destructive hover:text-destructive"
                                onClick={() => removeColumn(index)}
                                disabled={columns.length <= 1}
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </Button>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  ) : null}

                  {selectedSection === 'foreign_keys' ? (
                    <div className="grid grid-cols-[100px_1fr] items-start gap-x-3 gap-y-1">
                      <label className="pt-1.5 text-xs leading-8 text-muted-foreground">
                        {t('explorer.create_dialog_foreign_keys')}
                      </label>
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <Button type="button" variant="outline" size="sm" className="h-8 shrink-0 text-xs" onClick={addForeignKey}>
                            <Plus className="mr-1 h-3.5 w-3.5" />
                            {t('explorer.create_dialog_add_foreign_key')}
                          </Button>
                        </div>
                        <div className="max-h-44 overflow-y-auto rounded-md border border-input divide-y divide-border">
                          {foreignKeys.map((fk, index) => (
                            <div key={index} className="flex min-h-[36px] items-center gap-2 bg-muted/10 px-2 py-1">
                              <Input
                                value={fk.column}
                                onChange={(event) => updateForeignKey(index, 'column', event.target.value)}
                                placeholder={t('explorer.create_table_column_name')}
                                className="h-8 min-w-0 flex-1 text-sm"
                              />
                              <span className="shrink-0 text-muted-foreground">→</span>
                              <Input
                                value={fk.refTable}
                                onChange={(event) => updateForeignKey(index, 'refTable', event.target.value)}
                                placeholder={t('explorer.create_dialog_fk_ref_table')}
                                className="h-8 min-w-0 flex-1 text-sm"
                              />
                              <Input
                                value={fk.refColumn}
                                onChange={(event) => updateForeignKey(index, 'refColumn', event.target.value)}
                                placeholder="引用列"
                                className="h-8 min-w-0 flex-1 text-sm"
                              />
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 shrink-0 text-destructive"
                                onClick={() => removeForeignKey(index)}
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </Button>
                            </div>
                          ))}
                          {foreignKeys.length === 0 ? (
                            <div className="px-2 py-4 text-center text-xs text-muted-foreground">
                              {t('explorer.create_dialog_no_content')}
                            </div>
                          ) : null}
                        </div>
                      </div>
                    </div>
                  ) : null}

                  {selectedSection === 'indexes' ? (
                    <div className="grid grid-cols-[100px_1fr] items-start gap-x-3 gap-y-1">
                      <label className="pt-1.5 text-xs leading-8 text-muted-foreground">
                        {t('explorer.create_dialog_indexes')}
                      </label>
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <Button type="button" variant="outline" size="sm" className="h-8 shrink-0 text-xs" onClick={addIndex}>
                            <Plus className="mr-1 h-3.5 w-3.5" />
                            {t('explorer.create_dialog_add_index')}
                          </Button>
                        </div>
                        <div className="max-h-44 overflow-y-auto rounded-md border border-input divide-y divide-border">
                          {indexes.map((idx, index) => (
                            <div key={index} className="flex min-h-[36px] items-center gap-2 bg-muted/10 px-2 py-1">
                              <Input
                                value={idx.name}
                                onChange={(event) => updateIndex(index, 'name', event.target.value)}
                                placeholder={t('explorer.create_dialog_index_name')}
                                className="h-8 min-w-0 flex-1 text-sm"
                              />
                              <Input
                                value={idx.columns}
                                onChange={(event) => updateIndex(index, 'columns', event.target.value)}
                                placeholder={t('explorer.create_dialog_index_columns')}
                                className="h-8 min-w-0 flex-1 text-sm"
                              />
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 shrink-0 text-destructive"
                                onClick={() => removeIndex(index)}
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </Button>
                            </div>
                          ))}
                          {indexes.length === 0 ? (
                            <div className="px-2 py-4 text-center text-xs text-muted-foreground">
                              {t('explorer.create_dialog_no_content')}
                            </div>
                          ) : null}
                        </div>
                      </div>
                    </div>
                  ) : null}

                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_partition_expression')}</label>
                    <Input className="h-8 text-sm" />
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_partition_key')}</label>
                    <Input className="h-8 text-sm" />
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_options')}</label>
                    <div className="flex items-center gap-1">
                      <Input className="h-8 flex-1 text-sm" />
                      <Button variant="ghost" size="icon" className="h-8 w-8 shrink-0 opacity-60">...</Button>
                    </div>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_access_method')}</label>
                    <select className="h-8 flex-1 rounded-md border px-2 text-sm">
                      <option value="">-</option>
                    </select>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_tablespace')}</label>
                    <select className="h-8 flex-1 rounded-md border px-2 text-sm">
                      <option value="">-</option>
                    </select>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-center gap-x-3 gap-y-1">
                    <label className="text-xs text-muted-foreground">{t('explorer.create_dialog_owner')}</label>
                    <select className="h-8 flex-1 rounded-md border px-2 text-sm">
                      <option value="">-</option>
                    </select>
                  </div>
                  <div className="grid grid-cols-[100px_1fr] items-start gap-x-3 gap-y-1">
                    <label className="pt-1.5 text-xs text-muted-foreground">{t('explorer.create_dialog_authorization')}</label>
                    <div className="space-y-1">
                      <div className="flex items-center gap-1">
                        <Button variant="ghost" size="icon" className="h-7 w-7">
                          <Plus className="h-3.5 w-3.5" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-7 w-7">
                          <ChevronUp className="h-3.5 w-3.5" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-7 w-7">
                          <ChevronDown className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                      <div className="rounded-md border border-dashed border-border px-4 py-6 text-center text-xs text-muted-foreground">
                        {t('explorer.create_dialog_no_content')}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="shrink-0 border-t border-border">
            <button
              type="button"
              className="flex w-full items-center justify-between px-4 py-2 text-left text-sm hover:bg-muted/50"
              onClick={() => setPreviewExpanded((expanded) => !expanded)}
            >
              <span className="flex items-center gap-1">
                {t('explorer.create_dialog_preview')}
                <ChevronDownIcon className={cn('h-4 w-4 transition-transform', !previewExpanded && '-rotate-90')} />
              </span>
              <div className="flex items-center gap-2">
                <Button variant="ghost" size="icon" className="h-6 w-6">
                  <Settings className="h-3.5 w-3.5 opacity-70" />
                </Button>
                <CheckCircle2 className="h-4 w-4 text-green-500" />
              </div>
            </button>
            {previewExpanded ? (
              <div className="shrink-0 px-4 pb-3">
                <pre className="max-h-32 overflow-x-auto overflow-y-auto rounded-md border border-border bg-muted/30 p-3 font-mono text-xs">
                  {previewSql}
                </pre>
              </div>
            ) : null}
          </div>

          <div className="relative flex shrink-0 items-center justify-between border-t border-border px-4 py-3">
            <Button variant="ghost" size="icon" className="h-8 w-8">
              <HelpCircle className="h-4 w-4" />
            </Button>
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => handleClose(false)}>
                {t(I18N_KEYS.CONNECTIONS.CANCEL)}
              </Button>
              <Button disabled={isPending || !tableName.trim() || !connectionId} onClick={handleSubmit}>
                {isPending ? t(I18N_KEYS.CONNECTIONS.SAVING) : t(I18N_KEYS.EXPLORER.CREATE_DIALOG_CONFIRM)}
              </Button>
            </div>
          </div>

          <div
            className="absolute bottom-0 right-0 z-10 flex h-5 w-5 cursor-se-resize select-none items-end justify-end"
            onMouseDown={handleResizeMouseDown}
            title={t('explorer.create_dialog_resize')}
          >
            <svg className="mb-0.5 mr-0.5 h-3.5 w-3.5 text-muted-foreground/40" viewBox="0 0 16 16">
              <path d="M14 14v-4M14 14h-4" stroke="currentColor" strokeWidth="1.2" fill="none" strokeLinecap="round" />
            </svg>
          </div>
        </div>
      </DialogPortal>
    </Dialog>
  );
}
